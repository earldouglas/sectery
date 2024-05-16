package sectery.usecases.responders

import sectery.BuildInfo
import sectery.control.Monad
import sectery.domain.entities._
import sectery.usecases.Responder

class VersionResponder[F[_]: Monad] extends Responder[F]:

  override def name = "@version"

  override def usage = "@version"

  override def respondToMessage(rx: Rx) =
    rx match
      case Rx(c, _, "@version") =>
        summon[Monad[F]].pure(List(Tx(c, s"${BuildInfo.version}")))
      case _ =>
        summon[Monad[F]].pure(Nil)
