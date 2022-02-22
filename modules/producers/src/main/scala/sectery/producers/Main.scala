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
import zio.ZIO
import zio.ZLayer
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

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    catchAndLog {
      for
        producerFiber <- Producer.start(sqsInbox, sqsOutbox)
        _ <- producerFiber.join
      yield ExitCode.failure // should never exit
    }
      .provideLayer(ZEnv.any ++ Db.live ++ Http.live)
      .forever
