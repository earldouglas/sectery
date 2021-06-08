package sectery

import org.slf4j.LoggerFactory
import zio.App
import zio.ExitCode
import zio.RIO
import zio.URIO
import zio.ZEnv
import zio.ZIO
import zio.clock.Clock

object Sectery extends App:

  /**   1. Spin up the message queues using [[Bot]] as the [[Sender]] 2.
    *      Asynchronously process received messages through
    *      [[Bot.receive]] 3. Connect to IRC
    */
  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    val go: RIO[Producer.Env, Unit] =
      for
        inbox <- MessageQueues.loop(Bot)
        _ = Bot.receive = (m: Rx) => unsafeRunAsync_(inbox.offer(m))
        _ = Bot.start()
      yield ()
    go
      .provideLayer(ZEnv.any ++ Db.live ++ Http.live)
      .catchAll { e =>
        LoggerFactory
          .getLogger(this.getClass())
          .error("caught exception", e)
        ZIO.effectTotal(())
      }
      .map { _ =>
        ExitCode.failure // should never exit
      }
