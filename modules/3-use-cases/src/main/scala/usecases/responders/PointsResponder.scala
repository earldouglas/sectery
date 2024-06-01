package sectery.usecases.responders

import sectery.control.Monad
import sectery.control.Monad._
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases.Responder

class PointsResponder[F[_]: Monad: Points] extends Responder[F]:

  override def name = "++/--"

  override def usage = "<nick>++ | <nick>--"

  private val plusOne = """^\s*([^+]+)[+][+]\s*$""".r
  private val minusOne = """^\s*([^+]+)[-][-]\s*$""".r

  override def respondToMessage(rx: Rx) =
    rx.message match

      case plusOne(plusOneNick) if rx.nick == plusOneNick =>
        summon[Monad[F]].pure(
          List(
            Tx(
              service = rx.service,
              channel = rx.channel,
              thread = rx.thread,
              message = "You gotta get someone else to do it."
            )
          )
        )

      case plusOne(plusOneNick) if rx.nick != plusOneNick =>
        summon[Points[F]]
          .update(rx.service, rx.channel, plusOneNick, 1)
          .map { newValue =>
            List(
              Tx(
                service = rx.service,
                channel = rx.channel,
                thread = rx.thread,
                message = s"""${plusOneNick} has ${newValue} point${
                    if newValue == 1 then "" else "s"
                  }."""
              )
            )
          }

      case minusOne(minusOneNick) if rx.nick == minusOneNick =>
        summon[Monad[F]].pure(
          List(
            Tx(
              service = rx.service,
              channel = rx.channel,
              thread = rx.thread,
              message = "You gotta get someone else to do it."
            )
          )
        )

      case minusOne(minusOneNick) if rx.nick != minusOneNick =>
        summon[Points[F]]
          .update(rx.service, rx.channel, minusOneNick, -1)
          .map { newValue =>
            List(
              Tx(
                service = rx.service,
                channel = rx.channel,
                thread = rx.thread,
                message = s"""${minusOneNick} has ${newValue} point${
                    if newValue == 1 then "" else "s"
                  }."""
              )
            )
          }

      case _ =>
        summon[Monad[F]].pure(Nil)
