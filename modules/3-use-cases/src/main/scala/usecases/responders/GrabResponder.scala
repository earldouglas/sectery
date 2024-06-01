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
    rx.message match

      case grab(nick) if rx.nick == nick =>
        summon[Monad[F]].pure(
          List(
            Tx(
              service = rx.service,
              channel = rx.channel,
              thread = rx.thread,
              message = "You can't grab yourself."
            )
          )
        )

      case grab(nick) =>
        summon[Grab[F]]
          .grab(rx.service, rx.channel, nick)
          .map {
            case true =>
              List(
                Tx(
                  service = rx.service,
                  channel = rx.channel,
                  thread = rx.thread,
                  message = s"Grabbed ${nick}."
                )
              )
            case false =>
              List(
                Tx(
                  service = rx.service,
                  channel = rx.channel,
                  thread = rx.thread,
                  message = s"${nick} hasn't said anything."
                )
              )
          }

      case "@grab" =>
        summon[Grab[F]]
          .grab(rx.service, rx.channel)
          .map {
            case Some(nick) =>
              List(
                Tx(
                  service = rx.service,
                  channel = rx.channel,
                  thread = rx.thread,
                  message = s"Grabbed ${nick}."
                )
              )
            case None =>
              List(
                Tx(
                  service = rx.service,
                  channel = rx.channel,
                  thread = rx.thread,
                  message = "Nobody has said anything."
                )
              )
          }

      case _ =>
        summon[LastMessage[F]]
          .saveLastMessage(rx)
          .map { _ =>
            Nil
          }
