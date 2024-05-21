package sectery.usecases.announcers

import java.time.Instant
import java.util.Calendar
import java.util.TimeZone
import sectery.control.Monad
import sectery.control.Monad._
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases.Announcer

class AutoquoteAnnouncer[F[_]: Monad: Autoquote: Now]
    extends Announcer[F]:

  private def timeForAutoquote(now: Instant): Boolean =

    val calendar: Calendar =
      val tz = TimeZone.getTimeZone("America/Phoenix")
      val c = Calendar.getInstance(tz)
      c.setTimeInMillis(now.toEpochMilli())
      c

    val eightToFive: Boolean =
      val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
      hourOfDay >= 8 && hourOfDay < 17

    val mondayThroughFriday: Boolean =
      val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
      dayOfWeek == Calendar.MONDAY ||
      dayOfWeek == Calendar.TUESDAY ||
      dayOfWeek == Calendar.WEDNESDAY ||
      dayOfWeek == Calendar.THURSDAY ||
      dayOfWeek == Calendar.FRIDAY

    eightToFive && mondayThroughFriday

  override def announce(): F[List[Tx]] =
    summon[Now[F]]
      .now()
      .map(timeForAutoquote)
      .flatMap {
        case true =>
          summon[Autoquote[F]].getAutoquoteMessages()
        case false =>
          summon[Monad[F]].pure(Nil)
      }
