package sectery.producers

import sectery.Producer
import sectery.Rx
import sectery.Tx
import zio.Clock
import zio.ZIO

object Ping extends Producer:

  override def help(): Iterable[Info] =
    Some(Info("@ping", "@ping"))

  override def apply(m: Rx): ZIO[Clock, Nothing, Iterable[Tx]] =
    m match
      case Rx(c, _, "@ping") =>
        ZIO.succeed(Some(Tx(c, "pong")))
      case _ =>
        ZIO.succeed(None)
