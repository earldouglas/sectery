package sectery

import org.slf4j.LoggerFactory
import sectery.Producer
import sectery.Db
import sectery.Http
import sectery.irc.Bot
import zio.App
import zio.Clock
import zio.ExitCode
import zio.Fiber
import zio.Schedule
import zio.ZEnv
import zio.ZHub
import zio.ZIO
import zio.ZQueue
import zio.durationInt

/** This is the main entry point for the application. Set your env vars,
  * then run the `main` method.
  */
object Main extends App:

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    for
      inbox <- ZHub.sliding[Rx](100)
      outbox <- ZQueue.unbounded[Tx]
      producerFiber <- Producer.start(inbox, outbox)
      botFiber <- Bot.start(inbox, outbox)
      _ <- Fiber.joinAll(List(producerFiber, botFiber))
    yield ExitCode.failure // should never exit
  }.catchAllCause { cause =>
    LoggerFactory
      .getLogger(this.getClass())
      .error(cause.prettyPrint)
    ZIO.succeed(ExitCode.failure)
  }.provideLayer(ZEnv.any ++ Db.live ++ Http.live)
    .forever
