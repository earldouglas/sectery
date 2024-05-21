package sectery.usecases.responders

import sectery.PrettyTime
import sectery.control.Monad
import sectery.control.Monad._
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases.Responder

class TimeResponder[F[_]: Monad: GetConfig: Now] extends Responder[F]:

  override def name = "@time"

  override def usage =
    "@time [zone], e.g. @time, @time MST, @time GMT-7, @time America/Phoenix"

  private val time = """^@time(\s+([^\s]+)\s*)?$""".r

  private def getPrettyTimeAtZone(
      timeZoneString: String
  ): F[String] =
    summon[Now[F]]
      .now()
      .map(instant => PrettyTime(instant, timeZoneString))

  private def getResponse(
      channel: String,
      timeZoneString: String
  ): F[List[Tx]] =
    getPrettyTimeAtZone(timeZoneString)
      .map { message =>
        List(Tx(channel, message))
      }

  override def respondToMessage(rx: Rx) =
    rx match
      case Rx(c, nick, "@time") =>
        summon[GetConfig[F]]
          .getConfig(nick, "tz")
          .flatMap { timeZoneStringOption =>
            timeZoneStringOption match
              case Some(timeZoneString) =>
                getResponse(c, timeZoneString)
              case None =>
                val message =
                  s"${nick}: Set default time zone with `@set tz <zone>`"
                summon[Monad[F]].pure(List(Tx(c, message)))
          }
      case Rx(c, _, time(_, timeZoneString)) =>
        getResponse(c, timeZoneString)
      case _ =>
        summon[Monad[F]].pure(Nil)
