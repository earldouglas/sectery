package sectery.producers

import com.amazonaws.regions.Regions
import org.slf4j.LoggerFactory
import sectery.Db
import sectery.Http
import sectery.Producer
import sectery.Runtime.catchAndLog
import sectery.Rx
import sectery.SQSFifoQueue
import sectery.Tx
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
  ): ZIO[Clock, Nothing, Fiber[Throwable, Long]] = {
    for
      _ <- ZIO.succeed(
        LoggerFactory
          .getLogger(this.getClass())
          .debug(s"taking from outbox")
      )
      // txs <- outbox.takeAll
      tx <- outbox.take
      _ <- ZIO.succeed(
        LoggerFactory
          .getLogger(this.getClass())
          .debug(s"offering ${tx} to SQS outbox")
      )
      _ <- sqsOutbox.offer(Some(tx))
    yield ()
    // }.forever.fork
  }.repeat(Schedule.spaced(100.milliseconds)).fork

  def sqsInboxLoop(
      inbox: Hub[Rx]
  ): ZIO[Clock, Nothing, Fiber[Throwable, Long]] = {
    for
      _ <- ZIO.succeed(
        LoggerFactory
          .getLogger(this.getClass())
          .debug(s"taking from SQS inbox")
      )
      rxs <- sqsInbox.take()
      _ <- ZIO.succeed(
        LoggerFactory
          .getLogger(this.getClass())
          .debug(s"offering ${rxs} to inbox")
      )
      _ <- inbox.publishAll(rxs)
    yield ()
    // }.forever.fork
  }.repeat(Schedule.spaced(100.milliseconds)).fork

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    catchAndLog {
      for
        inbox <- ZHub.sliding[Rx](100)
        outbox <- ZQueue.unbounded[Tx]
        sqsInboxFiber <- sqsInboxLoop(inbox)
        sqsOutboxFiber <- sqsOutboxLoop(outbox)
        zFiber <- Producer.start(inbox, outbox)
        _ <- Fiber.joinAll(List(sqsInboxFiber, sqsOutboxFiber, zFiber))
      yield ExitCode.failure // should never exit
    }
      .provideLayer(ZEnv.any ++ Db.live ++ Http.live)
      .forever
