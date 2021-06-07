package sectery.producers

import sectery.Info
import sectery.Producer
import sectery.Rx
import sectery.Tx
import zio.URIO
import zio.ZIO
import zio.clock.Clock

object Ping extends Producer:

  override def help(): Iterable[Info] =
    Some(Info("@ping", "@ping"))

  override def apply(m: Rx): URIO[Clock, Iterable[Tx]] =
    m match
      case Rx(c, _, "@ping") =>
        ZIO.effectTotal(Some(Tx(c, "pong")))
      case _ =>
        ZIO.effectTotal(None)
