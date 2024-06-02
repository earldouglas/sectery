package sectery.usecases.responders

import sectery.control.Monad
import sectery.control.Monad._
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases.Responder

class GetResponder[F[_]: Monad: GetConfig] extends Responder[F]:

  override def name = "@get"

  override def usage = "@get <key>, e.g. @get tz"

  private val get = """^@get\s+([^\s]+)\s*$""".r

  override def respondToMessage(rx: Rx) =
    rx.message match
      case get(key) =>
        summon[GetConfig[F]]
          .getConfig(rx.nick, key)
          .map {
            _ match
              case Some(value) =>
                List(
                  Tx(
                    service = rx.service,
                    channel = rx.channel,
                    thread = rx.thread,
                    message = s"${rx.nick}: ${key} is set to ${value}"
                  )
                )
              case None =>
                List(
                  Tx(
                    service = rx.service,
                    channel = rx.channel,
                    thread = rx.thread,
                    message = s"${rx.nick}: ${key} is not set"
                  )
                )
          }
      case _ =>
        summon[Monad[F]].pure(Nil)
