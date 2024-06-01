package sectery.usecases.responders

import sectery.control.Monad
import sectery.control.Monad._
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases.Responder

class FrinkiacResponder[F[_]: Monad: Frinkiac] extends Responder[F]:

  override def name = "@frinkiac"

  override def usage =
    "@frinkiac <quote>, e.g. @frinkiac i got to think of a lie fast"

  private val frinkiac = """^@frinkiac\s+(.+)\s*$""".r

  override def respondToMessage(rx: Rx) =
    rx.message match
      case frinkiac(q) =>
        summon[Frinkiac[F]]
          .frinkiac(q)
          .map { messages =>
            messages.map { message =>
              Tx(
                service = rx.service,
                channel = rx.channel,
                thread = rx.thread,
                message = message
              )
            }
          }
      case _ =>
        summon[Monad[F]].pure(Nil)
