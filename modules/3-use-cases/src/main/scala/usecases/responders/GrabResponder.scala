package sectery.usecases.responders

import sectery.control.Monad
import sectery.control.Monad._
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases.Responder

class GrabResponder[F[_]: Monad: Grab: LastMessage]
    extends Responder[F]:

  override def name = "@grab"

  override def usage = "grab [nick]"

  private val grab = """^@grab\s+([^\s]+)\s*$""".r

  override def respondToMessage(rx: Rx) =
    rx match

      case Rx(c, from, grab(nick)) if from == nick =>
        summon[Monad[F]].pure(List(Tx(c, "You can't grab yourself.")))

      case Rx(c, _, grab(nick)) =>
        summon[Grab[F]]
          .grab(c, nick)
          .map {
            case true =>
              List(Tx(c, s"Grabbed ${nick}."))
            case false =>
              List(Tx(c, s"${nick} hasn't said anything."))
          }

      case Rx(c, _, "@grab") =>
        summon[Grab[F]]
          .grab(c)
          .map {
            case Some(nick) =>
              List(Tx(c, s"Grabbed ${nick}."))
            case None =>
              List(Tx(c, "Nobody has said anything."))
          }

      case _ =>
        summon[LastMessage[F]]
          .saveLastMessage(rx)
          .map { _ =>
            Nil
          }
