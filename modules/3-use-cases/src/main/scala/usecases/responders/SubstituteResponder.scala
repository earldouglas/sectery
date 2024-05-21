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
      channel: String,
      toReplace: String,
      howReplace: String => String
  ): F[List[Tx]] =
    val matcher: Regex = new Regex(s".*${toReplace}.*")
    summon[LastMessage[F]]
      .getLastMessages(channel)
      .map { rxs =>
        rxs
          .find(rx => matcher.matches(rx.message))
          .map { rx =>
            val replacedMsg: String = howReplace(rx.message)
            Tx(channel, s"<${rx.nick}> ${replacedMsg}")
          }
          .toList
      }

  override def respondToMessage(rx: Rx) =
    rx match
      case Rx(channel, nick, subFirst(toReplace, withReplace)) =>
        substitute(
          channel,
          toReplace,
          _.replaceFirst(toReplace, withReplace)
        )
      case Rx(channel, nick, subAll(toReplace, withReplace)) =>
        substitute(
          channel,
          toReplace,
          _.replaceAll(toReplace, withReplace)
        )
      case rx =>
        summon[LastMessage[F]]
          .saveLastMessage(rx)
          .map(_ => Nil)
