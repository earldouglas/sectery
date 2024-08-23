package sectery.usecases.responders

import scala.util.matching.Regex
import sectery.control.Monad
import sectery.control.Monad._
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases.Responder

class SubstituteResponder[F[_]: Monad: LastMessage]
    extends Responder[F]:

  override def name = "s///"

  override def usage = "s/find/replace/[g]"

  val subFirst = """s\/(.*)\/(.*)\/$""".r
  val subAll = """s\/(.*)\/(.*)\/g""".r

  private def substitute(
      nick: String,
      service: String,
      channel: String,
      thread: Option[String],
      toReplace: String,
      howReplace: String => String
  ): F[List[Tx]] =
    val matcher: Regex = new Regex(s".*${toReplace}.*")
    summon[LastMessage[F]]
      .getLastMessages(service, channel)
      .map { rxs =>
        rxs
          .find { rx =>
            rx.nick != nick &&
            matcher.matches(rx.message)
          }
          .map { rx =>
            val replacedMsg: String = howReplace(rx.message)
            Tx(
              service = service,
              channel = channel,
              thread = thread,
              message = s"<${rx.nick}> ${replacedMsg}"
            )
          }
          .toList
      }

  override def respondToMessage(rx: Rx) =
    rx.message match
      case subFirst(toReplace, withReplace) =>
        substitute(
          nick = rx.nick,
          service = rx.service,
          channel = rx.channel,
          thread = rx.thread,
          toReplace,
          _.replaceFirst(toReplace, withReplace)
        )
      case subAll(toReplace, withReplace) =>
        substitute(
          nick = rx.nick,
          service = rx.service,
          channel = rx.channel,
          thread = rx.thread,
          toReplace,
          _.replaceAll(toReplace, withReplace)
        )
      case _ =>
        summon[LastMessage[F]]
          .saveLastMessage(rx)
          .map(_ => Nil)
