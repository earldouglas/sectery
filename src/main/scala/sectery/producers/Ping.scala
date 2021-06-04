package sectery.producers

import sectery.Producer
import sectery.Rx
import sectery.Tx
import zio.clock.Clock
import zio.URIO
import zio.ZIO

object Ping extends Producer:

  override def help(): Iterable[String] =
    Some("@ping")

  override def apply(m: Rx): URIO[Clock, Iterable[Tx]] =
    m match
      case Rx(c, _, "@ping") =>
        ZIO.effectTotal(Some(Tx(c, "pong")))
      case _ =>
        ZIO.effectTotal(None)
