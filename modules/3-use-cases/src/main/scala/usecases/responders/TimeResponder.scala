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
      service: String,
      channel: String,
      thread: Option[String],
      timeZoneString: String
  ): F[List[Tx]] =
    getPrettyTimeAtZone(timeZoneString)
      .map { message =>
        List(
          Tx(
            service = service,
            channel = channel,
            thread = thread,
            message = message
          )
        )
      }

  override def respondToMessage(rx: Rx) =
    rx.message match
      case "@time" =>
        summon[GetConfig[F]]
          .getConfig(rx.nick, "tz")
          .flatMap { timeZoneStringOption =>
            timeZoneStringOption match
              case Some(timeZoneString) =>
                getResponse(
                  service = rx.service,
                  channel = rx.channel,
                  thread = rx.thread,
                  timeZoneString
                )
              case None =>
                val message =
                  s"${rx.nick}: Set default time zone with `@set tz <zone>`"
                summon[Monad[F]].pure(
                  List(
                    Tx(
                      service = rx.service,
                      channel = rx.channel,
                      thread = rx.thread,
                      message = message
                    )
                  )
                )
          }
      case time(_, timeZoneString) =>
        getResponse(
          service = rx.service,
          channel = rx.channel,
          thread = rx.thread,
          timeZoneString
        )
      case _ =>
        summon[Monad[F]].pure(Nil)
