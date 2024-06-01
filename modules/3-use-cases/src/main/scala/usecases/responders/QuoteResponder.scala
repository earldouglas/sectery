package sectery.usecases.responders

import sectery.control.Monad
import sectery.control.Monad._
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases.Responder

class QuoteResponder[F[_]: Monad: Quote] extends Responder[F]:

  override def name = "@quote"

  override def usage = "quote [nick]"

  private val quote = """^@quote\s+([^\s]+)\s*$""".r

  override def respondToMessage(rx: Rx) =
    rx.message match

      case quote(nick) =>
        summon[Quote[F]]
          .quote(rx.service, rx.channel, nick)
          .map {
            case Some(gm) =>
              List(
                Tx(
                  service = rx.service,
                  channel = rx.channel,
                  thread = rx.thread,
                  message = s"<${gm.nick}> ${gm.message}"
                )
              )
            case None =>
              List(
                Tx(
                  service = rx.service,
                  channel = rx.channel,
                  thread = rx.thread,
                  message = s"${nick} hasn't been grabbed."
                )
              )
          }

      case "@quote" =>
        summon[Quote[F]]
          .quote(rx.service, rx.channel)
          .map {
            case Some(gm) =>
              List(
                Tx(
                  service = rx.service,
                  channel = rx.channel,
                  thread = rx.thread,
                  message = s"<${gm.nick}> ${gm.message}"
                )
              )
            case None =>
              List(
                Tx(
                  service = rx.service,
                  channel = rx.channel,
                  thread = rx.thread,
                  message = s"Nobody has been grabbed."
                )
              )
          }

      case _ =>
        summon[Monad[F]].pure(Nil)
