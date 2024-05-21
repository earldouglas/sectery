package sectery.usecases.responders

import sectery.control.Monad
import sectery.control.Monad._
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases.Responder

class CountResponder[F[_]: Monad: Counter] extends Responder[F]:

  override def name = "@count"

  override def usage = "@count"

  override def respondToMessage(rx: Rx) =
    rx match
      case Rx(c, _, "@count") =>
        summon[Counter[F]]
          .incrementAndGet()
          .map { count =>
            List(Tx(c, s"${count}"))
          }
      case _ =>
        summon[Monad[F]].pure(Nil)
