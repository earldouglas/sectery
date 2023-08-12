package sectery.irc

import sectery.RabbitMQ
import sectery.Runtime.catchAndLog
import zio.Clock
import zio.ExitCode
import zio.ZIO
import zio.ZIOAppDefault
import zio.ZLayer

/** This is the main entry point for the module. Set your env vars, then
  * run the `main` method.
  */
object Main extends ZIOAppDefault:

  override def run: ZIO[Any, Nothing, ExitCode] =
    catchAndLog {
      for
        botFiber <- Bot.start
        _ <- botFiber.join
      yield ()
    }
      .provide(ZLayer.succeed(Clock.ClockLive) ++ RabbitMQ.live)
      .forever
      .map(_ => ExitCode.failure) // should never exit
