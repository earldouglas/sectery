package sectery.producers

import sectery.Db
import sectery.Http
import sectery.Producer
import sectery.RabbitMQ
import sectery.Runtime.catchAndLog
import zio.Clock
import zio.ExitCode
import zio.ZIO
import zio.ZIOAppDefault
import zio.ZLayer
import zio.openai.Completions

/** This is the main entry point for the module. Set your env vars, then
  * run the `main` method.
  */
object Main extends ZIOAppDefault:

  override def run: ZIO[Any, Throwable, ExitCode] =
    catchAndLog {
      for
        producerFiber <- Producer.start
        _ <- producerFiber.join
      yield ()
    }
      .provide(
        ZLayer.succeed(
          Clock.ClockLive
        ) ++ RabbitMQ.live ++ Db.live ++ Http.live ++ Completions.default
      )
      .forever
      .map(_ => ExitCode.failure) // should never exit
