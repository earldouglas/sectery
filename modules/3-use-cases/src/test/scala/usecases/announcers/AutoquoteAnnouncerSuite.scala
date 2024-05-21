package sectery.usecases.announcers

import java.time.Instant
import java.util.Calendar
import java.util.TimeZone
import munit.FunSuite
import munit.ScalaCheckSuite
import org.scalacheck.Prop._
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class AutoquoteAnnouncerSuite extends ScalaCheckSuite:

  private def getTimeMillis(
      year: Int,
      week: Int,
      day: Int,
      hour: Int,
      minute: Int,
      second: Int
  ): Long =
    new Calendar.Builder()
      .setTimeZone(TimeZone.getTimeZone("America/Phoenix"))
      .setWeekDate(year, week, day)
      .setTimeOfDay(hour, minute, second)
      .build()
      .getTimeInMillis()

  property("Autoquote does not run in the morning") {
    forAll { (_year: Int, _week: Int, _day: Int, _hour: Int) =>

      val timeMillis: Long =
        getTimeMillis(
          year = math.abs(_year % 100) + 1970,
          week = math.abs(_week % 52) + 1, // 1-52
          day = math.abs(_day % 7) + 1, // 1-7
          hour = math.abs(_hour % 8), // 0-7
          minute = 0,
          second = 0
        )

      given now: Now[Id] with
        override def now() = Instant.ofEpochMilli(timeMillis)

      given autoquote: Autoquote[Id] with
        override def getAutoquoteMessages() = List(Tx("#foo", "bar"))

      assertEquals(
        obtained = new AutoquoteAnnouncer[Id].announce(),
        expected = Nil
      )
    }
  }

  property("Autoquote does not run in the evening") {
    forAll { (_year: Int, _week: Int, _day: Int, _hour: Int) =>

      val timeMillis: Long =
        getTimeMillis(
          year = math.abs(_year % 100) + 1970,
          week = math.abs(_week % 52) + 1, // 1-52
          day = math.abs(_day % 7) + 1, // 1-7
          hour = math.abs(_hour % 7) + 17, // 17-23
          minute = 0,
          second = 0
        )

      given now: Now[Id] with
        override def now() = Instant.ofEpochMilli(timeMillis)

      given autoquote: Autoquote[Id] with
        override def getAutoquoteMessages() = List(Tx("#foo", "bar"))

      assertEquals(
        obtained = new AutoquoteAnnouncer[Id].announce(),
        expected = Nil
      )
    }
  }

  property("Autoquote does not run on the weekend") {
    forAll { (_year: Int, _week: Int, _day: Int, _hour: Int) =>

      val timeMillis: Long =
        getTimeMillis(
          year = math.abs(_year % 100) + 1970,
          week = math.abs(_week % 52) + 1, // 1-52
          day = if _day < 0 then 1 else 7,
          hour = math.abs(_hour % 24), // 0-23
          minute = 0,
          second = 0
        )

      given now: Now[Id] with
        override def now() = Instant.ofEpochMilli(timeMillis)

      given autoquote: Autoquote[Id] with
        override def getAutoquoteMessages() = List(Tx("#foo", "bar"))

      assertEquals(
        obtained = new AutoquoteAnnouncer[Id].announce(),
        expected = Nil
      )
    }
  }

  property("Autoquote runs during the weekday") {
    forAll { (_year: Int, _week: Int, _day: Int, _hour: Int) =>

      val timeMillis: Long =
        getTimeMillis(
          year = math.abs(_year % 100) + 1970,
          week = math.abs(_week % 52) + 1, // 1-52
          day = math.abs(_day % 5) + 2, // 2-6
          hour = math.abs(_hour % 9) + 8, // 8-16
          minute = 0,
          second = 0
        )

      given now: Now[Id] with
        override def now() = Instant.ofEpochMilli(timeMillis)

      given autoquote: Autoquote[Id] with
        override def getAutoquoteMessages() = List(Tx("#foo", "bar"))

      assertEquals(
        obtained = new AutoquoteAnnouncer[Id].announce(),
        List(Tx("#foo", "bar"))
      )
    }
  }
