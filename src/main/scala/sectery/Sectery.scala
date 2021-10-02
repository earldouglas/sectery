package sectery

import org.slf4j.LoggerFactory
import zio.App
import zio.Clock
import zio.ExitCode
import zio.Fiber
import zio.RIO
import zio.URIO
import zio.ZEnv
import zio.ZIO

object Sectery extends App:

  /** This is the main entry point for the application. Set your env
    * vars, then run this main method.
    *
    *   1. Spin up the message queues using [[Bot]] as the [[Sender]]
    *   1. Asynchronously process received messages through
    *      [[Bot.receive]]
    *   1. Connect to IRC
    */
  def run(args: List[String]): URIO[ZEnv, ExitCode] =

    val k1: RIO[Producer.Env, ExitCode] =
      for
        (inbox, fibers) <- MessageQueues.loop(Bot)
        _ = Bot.receive = (m: Rx) => unsafeRunAsync(inbox.offer(m))
        _ <- Bot.start()
        _ <- Fiber.joinAll(fibers)
      yield ExitCode.failure // should never exit

    val k0: URIO[Producer.Env, ExitCode] =
      k1
        .catchAll { e =>
          LoggerFactory
            .getLogger(this.getClass())
            .error("caught exception", e)
          ZIO.succeed(ExitCode.failure)
        }

    val k: URIO[ZEnv, ExitCode] =
      k0.provideLayer(ZEnv.any ++ Db.live ++ Http.live)

    k.forever
