package sectery.usecases.responders

import sectery.control.Monad
import sectery.domain.entities._
import sectery.usecases.Responder

class PingResponder[F[_]: Monad] extends Responder[F]:

  override def name = "@ping"

  override def usage = "@ping"

  override def respondToMessage(rx: Rx) =
    rx match
      case Rx(c, _, "@ping") =>
        summon[Monad[F]].pure(List(Tx(c, "pong")))
      case _ =>
        summon[Monad[F]].pure(Nil)
