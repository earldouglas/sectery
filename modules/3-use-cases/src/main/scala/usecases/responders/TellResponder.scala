package sectery.usecases.responders

import org.ocpsoft.prettytime.PrettyTime
import sectery.control.Monad
import sectery.control.Monad._
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases.Responder

class TellResponder[F[_]: Monad: Tell: Now] extends Responder[F]:

  override def name = "@tell"

  override def usage =
    "@tell <nick> <message>, e.g. @tell bob remember the milk"

  private val tell = """^@tell\s+([^\s]+)\s+(.+)\s*$""".r

  override def respondToMessage(rx: Rx) =
    rx match

      case Rx(c, from, tell(to, message)) =>
        summon[Now[F]]
          .now()
          .flatMap { now =>
            val m =
              Tell.Saved(
                to = to,
                from = from,
                date = now,
                message = message
              )
            summon[Tell[F]]
              .save(c, m)
              .map { _ =>
                List(Tx(c, s"I will let ${to} know."))
              }
          }

      case Rx(c, nick, _) =>
        summon[Tell[F]]
          .pop(c, nick)
          .map { saveds =>
            saveds.map { saved =>
              val prettyTime = new PrettyTime().format(saved.date)
              val message =
                s"${saved.to}: ${prettyTime}, ${saved.from} said: ${saved.message}"
              Tx(c, message)
            }
          }

      case _ =>
        summon[Monad[F]].pure(Nil)
