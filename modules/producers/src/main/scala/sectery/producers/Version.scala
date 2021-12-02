package sectery.producers

import sectery.BuildInfo
import sectery.Producer
import sectery.Rx
import sectery.Tx
import zio.ZIO

object Version extends Producer:

  override def help(): Iterable[Info] =
    Some(Info("@version", "@version"))

  override def apply(m: Rx): ZIO[Any, Nothing, Iterable[Tx]] =
    m match
      case Rx(channel, _, "@version") =>
        ZIO.succeed(
          Some(Tx(channel, s"${BuildInfo.version}"))
        )
      case _ =>
        ZIO.succeed(None)
