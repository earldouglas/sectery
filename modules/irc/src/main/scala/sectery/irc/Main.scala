package sectery.irc

import com.amazonaws.regions.Regions
import org.slf4j.LoggerFactory
import sectery.Runtime.catchAndLog
import sectery.Rx
import sectery.SQSFifoQueue
import sectery.Tx
import sectery.irc.Bot
import zio.App
import zio.Clock
import zio.ExitCode
import zio.Fiber
import zio.Hub
import zio.Queue
import zio.Schedule
import zio.ZEnv
import zio.ZHub
import zio.ZIO
import zio.ZLayer
import zio.ZQueue
import zio.durationInt
import zio.json._

/** This is the main entry point for the module. Set your env vars, then
  * run the `main` method.
  */
object Main extends App:

  val sqsInbox =
    new SQSFifoQueue[Rx](
      Regions.US_EAST_2,
      sys.env("SQS_INBOX_URL"),
      "sectery-inbox"
    )(DeriveJsonCodec.gen[Rx])

  val sqsOutbox =
    new SQSFifoQueue[Tx](
      Regions.US_EAST_2,
      sys.env("SQS_OUTBOX_URL"),
      "sectery-outbox"
    )(DeriveJsonCodec.gen[Tx])

  def sqsOutboxLoop(
      outbox: Queue[Tx]
  ): ZIO[Any, Nothing, Fiber[Throwable, Nothing]] = {
    for
      _ <- ZIO.succeed(
        LoggerFactory
          .getLogger(this.getClass())
          .debug(s"taking from SQS outbox")
      )
      txs <- sqsOutbox.take()
      _ <- ZIO.succeed(
        LoggerFactory
          .getLogger(this.getClass())
          .debug(s"offering ${txs} to outbox")
      )
      _ <- outbox.offerAll(txs)
    yield ()
  }.forever.fork

  def sqsInboxLoop(
      inbox: Hub[Rx]
  ): ZIO[Any, Nothing, Fiber[Throwable, Nothing]] =
    for
      p <- zio.Promise.make[Nothing, Unit]
      fiber <-
        inbox.subscribe.use { case q =>
          {
            for
              _ <- p.succeed(())
              _ <- ZIO.succeed(
                LoggerFactory
                  .getLogger(this.getClass())
                  .debug(s"taking from inbox")
              )
              rx <- q.take
              _ <- ZIO.succeed(
                LoggerFactory
                  .getLogger(this.getClass())
                  .debug(s"offering Some(${rx}) to SQS inbox")
              )
              _ <- sqsInbox.offer(Some(rx))
            yield ()
          }.forever
        }.fork
      _ <- p.await
    yield fiber

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    catchAndLog {
      for
        inbox <- ZHub.sliding[Rx](100)
        outbox <- ZQueue.unbounded[Tx]
        sqsInboxFiber <- sqsInboxLoop(inbox)
        sqsOutboxFiber <- sqsOutboxLoop(outbox)
        zFiber <- Bot.start(inbox, outbox)
        _ <- Fiber.joinAll(List(sqsInboxFiber, sqsOutboxFiber, zFiber))
      yield ExitCode.failure // should never exit
    }
      .provideLayer(ZEnv.any)
      .forever
