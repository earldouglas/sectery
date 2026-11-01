package sectery.usecases.responders

import sectery.control.Monad
import sectery.control.Monad._
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases.Responder

class GeodeResponder[F[_]: Monad: Geode] extends Responder[F]:

  override def name = "@geode"

  override def usage = "@geode <ip>, e.g. @geode 1.1.1.1"

  private val geode = """^@geode\s+(\d+\.\d+\.\d+\.\d+)\s*$""".r

  override def respondToMessage(rx: Rx) =
    rx.message match
      case geode(ip) =>
        summon[Geode[F]]
          .geode(ip)
          .map {
            case Right(x) => x
            case Left(x)  => x
          }
          .map { data =>
            List(
              Tx(
                service = rx.service,
                channel = rx.channel,
                thread = rx.thread,
                message = data.toString
              )
            )
          }
      case _ =>
        summon[Monad[F]].pure(Nil)
