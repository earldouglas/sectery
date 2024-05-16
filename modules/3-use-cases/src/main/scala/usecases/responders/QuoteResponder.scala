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
    rx match

      case Rx(c, _, quote(nick)) =>
        summon[Quote[F]]
          .quote(c, nick)
          .map {
            case Some(gm) =>
              List(Tx(c, s"<${gm.nick}> ${gm.message}"))
            case None =>
              List(Tx(c, s"${nick} hasn't been grabbed."))
          }

      case Rx(c, _, "@quote") =>
        summon[Quote[F]]
          .quote(c)
          .map {
            case Some(gm) =>
              List(Tx(c, s"<${gm.nick}> ${gm.message}"))
            case None =>
              List(Tx(c, s"Nobody has been grabbed."))
          }

      case _ =>
        summon[Monad[F]].pure(Nil)
