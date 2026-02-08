package sectery.usecases.responders

import sectery.control.Monad
import sectery.control.Monad._
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases.Responder

class MorbotronResponder[F[_]: Monad: Morbotron] extends Responder[F]:

  override def name = "@morbotron"

  override def usage =
    "@morbotron <quote>, e.g. @morbotron i got to think of a lie fast"

  private val morbotron = """^@morbotron\s+(.+)\s*$""".r

  override def respondToMessage(rx: Rx) =
    rx.message match
      case morbotron(q) =>
        summon[Morbotron[F]]
          .morbotron(q)
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
