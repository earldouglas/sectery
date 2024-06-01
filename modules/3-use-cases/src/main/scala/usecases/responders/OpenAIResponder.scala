package sectery.usecases.responders

import sectery.control.Monad
import sectery.control.Monad._
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases.Responder

class OpenAIResponder[F[_]: Monad: OpenAI] extends Responder[F]:

  override def name = "@ai"

  override def usage = "@ai <prompt>"

  private val ai = """^@ai\s+(.+)$""".r

  override def respondToMessage(rx: Rx) =
    rx.message match
      case ai(prompt) =>
        summon[OpenAI[F]]
          .complete(prompt)
          .map { completions =>
            completions.map { completion =>
              Tx(
                service = rx.service,
                channel = rx.channel,
                thread = rx.thread,
                message = completion
              )
            }
          }
      case _ =>
        summon[Monad[F]].pure(Nil)
