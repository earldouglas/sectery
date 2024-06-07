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

  private def grabLastMessage(
      service: String,
      channel: String,
      thread: Option[String],
      nick: String
  ): F[List[Tx]] =
    summon[Grab[F]]
      .grab(service, channel, nick)
      .map {
        case true =>
          List(
            Tx(
              service = service,
              channel = channel,
              thread = thread,
              message = s"Grabbed ${nick}."
            )
          )
        case false =>
          List(
            Tx(
              service = service,
              channel = channel,
              thread = thread,
              message = s"${nick} hasn't said anything."
            )
          )
      }

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
        grabLastMessage(
          service = rx.service,
          channel = rx.channel,
          thread = rx.thread,
          nick = nick
        )

      case "@grab" =>
        summon[LastMessage[F]]
          .getLastMessages(rx.service, rx.channel)
          .flatMap(lms =>
            lms
              .filter(lm => lm.nick != rx.nick)
              .headOption match
              case Some(lm) =>
                grabLastMessage(
                  service = rx.service,
                  channel = rx.channel,
                  thread = rx.thread,
                  nick = lm.nick
                )
              case None =>
                summon[Monad[F]]
                  .pure(
                    List(
                      Tx(
                        service = rx.service,
                        channel = rx.channel,
                        thread = rx.thread,
                        message = "Nobody has said anything."
                      )
                    )
                  )
          )

      case _ =>
        summon[LastMessage[F]]
          .saveLastMessage(rx)
          .map { _ =>
            Nil
          }
