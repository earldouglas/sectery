package sectery.usecases.responders

import sectery.control.Monad
import sectery.control.Monad._
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases.Responder

class SetResponder[F[_]: Monad: SetConfig] extends Responder[F]:

  override def name = "@set"

  override def usage =
    "@set <key> <value>, e.g. @set tz America/New_York"

  private val set = """^@set\s+([^\s]+)\s+(.+)\s*$""".r

  override def respondToMessage(rx: Rx) =
    rx.message match
      case set(key, value) =>
        summon[SetConfig[F]]
          .setConfig(rx.nick, key, value)
          .map { _ =>
            List(
              Tx(
                service = rx.service,
                channel = rx.channel,
                thread = rx.thread,
                message = s"${rx.nick}: ${key} set to ${value}"
              )
            )
          }
      case _ =>
        summon[Monad[F]].pure(Nil)
