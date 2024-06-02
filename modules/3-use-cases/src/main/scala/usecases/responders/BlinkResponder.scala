package sectery.usecases.responders

import sectery.control.Monad
import sectery.domain.entities._
import sectery.usecases.Responder

class BlinkResponder[F[_]: Monad] extends Responder[F]:

  override def name = "@blink"

  override def usage = "@blink <text>"

  private val blink = """^@blink\s(.+)$""".r

  override def respondToMessage(rx: Rx) =
    rx.message match
      case blink(text) =>
        val b = '\u0006'
        summon[Monad[F]].pure(
          List(
            Tx(
              service = rx.service,
              channel = rx.channel,
              thread = rx.thread,
              message = s"${b}${text}${b}"
            )
          )
        )
      case _ =>
        summon[Monad[F]].pure(Nil)
