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
    rx match

      case Rx(channel, fromNick, plusOne(plusOneNick))
          if fromNick == plusOneNick =>
        summon[Monad[F]].pure(
          List(Tx(channel, "You gotta get someone else to do it."))
        )

      case Rx(channel, fromNick, plusOne(plusOneNick))
          if fromNick != plusOneNick =>
        summon[Points[F]]
          .update(channel, plusOneNick, 1)
          .map { newValue =>
            List(
              Tx(
                channel,
                s"""${plusOneNick} has ${newValue} point${
                    if newValue == 1 then "" else "s"
                  }."""
              )
            )
          }

      case Rx(channel, fromNick, minusOne(minusOneNick))
          if fromNick == minusOneNick =>
        summon[Monad[F]].pure(
          List(Tx(channel, "You gotta get someone else to do it."))
        )

      case Rx(channel, fromNick, minusOne(minusOneNick))
          if fromNick != minusOneNick =>
        summon[Points[F]]
          .update(channel, minusOneNick, -1)
          .map { newValue =>
            List(
              Tx(
                channel,
                s"""${minusOneNick} has ${newValue} point${
                    if newValue == 1 then "" else "s"
                  }."""
              )
            )
          }

      case _ =>
        summon[Monad[F]].pure(Nil)
