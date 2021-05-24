package sectery

import zio.App
import zio.ExitCode
import zio.UIO
import zio.URIO
import zio.ZEnv
import zio.ZIO

object Sectery extends App:

  /**
   * 1. Spin up the message queues using [[Bot]] as the [[Sender]]
   * 2. Asynchronously process received messages through [[Bot.receive]]
   * 3. Connect to IRC
   */
  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    for
      inbox <- sectery.MessageQueues.loop(Bot)
      _      = Bot.receive = (m: Rx) => unsafeRunAsync_(inbox.offer(m))
      _      = Bot.start()
    yield ExitCode.failure // should never exit
