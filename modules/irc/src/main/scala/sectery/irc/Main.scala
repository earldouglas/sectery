package sectery.irc

import org.slf4j.LoggerFactory
import sectery.Runtime.catchAndLog
import sectery.Rx
import sectery.SqsQueue
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
    new SqsQueue[Rx](
      queueUrl = sys.env("SQS_INBOX_URL"),
      messageGroupId = "sectery-inbox"
    )(DeriveJsonCodec.gen[Rx])

  val sqsOutbox =
    new SqsQueue[Tx](
      queueUrl = sys.env("SQS_OUTBOX_URL"),
      messageGroupId = "sectery-outbox"
    )(DeriveJsonCodec.gen[Tx])

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    catchAndLog {
      for
        botFiber <- Bot.start(sqsInbox, sqsOutbox)
        _ <- botFiber.join
      yield ExitCode.failure // should never exit
    }
      .provideLayer(ZEnv.any)
      .forever
