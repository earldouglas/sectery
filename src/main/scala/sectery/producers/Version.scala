package sectery.producers

import sectery.BuildInfo
import sectery.Info
import sectery.Producer
import sectery.Rx
import sectery.Tx
import zio.UIO
import zio.ZIO

object Version extends Producer:

  override def help(): Iterable[Info] =
    Some(Info("@version", "@version"))

  override def apply(m: Rx): UIO[Iterable[Tx]] =
    m match
      case Rx(channel, _, "@version") =>
        ZIO.effectTotal(
          Some(Tx(channel, s"${BuildInfo.version}"))
        )
      case _ =>
        ZIO.effectTotal(None)
