package sectery

import java.sql.Connection
import java.sql.Timestamp
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import sectery.producers.Grab
import sectery.Runtime.catchAndLog
import zio.Clock
import zio.ZIO

object Autoquote:

  private def timeForAutoquote(nowMillis: Long): Boolean =

    val calendar: Calendar =
      val tz = TimeZone.getTimeZone("America/Phoenix")
      val c = Calendar.getInstance(tz)
      c.setTimeInMillis(nowMillis)
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

  private def getAutoquoteChannels(
      nowMillis: Long
  ): ZIO[Db.Db, Throwable, List[String]] =

    def getChannels(conn: Connection): List[String] =
      val s =
        """|SELECT CHANNEL
           |FROM AUTOQUOTE
           |WHERE TIMESTAMP <= ?
           |""".stripMargin
      val stmt = conn.prepareStatement(s)
      val fourtyFiveMinutesAgoMillis = nowMillis - 45L * 60L * 1000L
      stmt.setTimestamp(1, new Timestamp(fourtyFiveMinutesAgoMillis))
      val rs = stmt.executeQuery
      var cs: List[String] = Nil
      if rs.next() then
        val channel = rs.getString("CHANNEL")
        cs = channel :: cs
      stmt.close()
      cs

    def updateAutoquote(
        channels: List[String]
    )(conn: Connection): Unit =
      if channels.length > 0 then
        val in = channels.map(_ => "?").mkString(", ")
        val s =
          s"""|UPDATE AUTOQUOTE
              |SET TIMESTAMP = ?
              |WHERE CHANNEL IN (${in})
              |""".stripMargin
        val stmt = conn.prepareStatement(s)
        stmt.setTimestamp(1, new Timestamp(nowMillis))
        channels.zipWithIndex.map { case (channel, index) =>
          stmt.setString(2 + index, channel)
        }
        stmt.execute()
        stmt.close()
      else ()

    Db { conn =>
      val channels = getChannels(conn)
      updateAutoquote(channels)(conn)
      channels
    }

  private def getAutoquoteMessages(
      channels: List[String]
  ): ZIO[Db.Db, Throwable, List[Tx]] =
    ZIO
      .collectAll(
        channels.map { channel =>
          Grab.randomGrabbedMessage(channel).map { gmo =>
            gmo.map { gm =>
              Tx(
                channel = channel,
                message = s"<${gm.nick}> ${gm.message}"
              )
            }
          }
        }
      )
      .map(_.flatten)

  private def unsafeAutoquote(
      outbox: Queue[Tx]
  ): ZIO[Clock with Db.Db, Throwable, Unit] =
    for
      nowMillis <- Clock.currentTime(TimeUnit.MILLISECONDS)
      _ <-
        if timeForAutoquote(nowMillis) then
          for
            channels <- getAutoquoteChannels(nowMillis)
            messages <- getAutoquoteMessages(channels)
            _ <- outbox.offer(messages)
          yield ()
        else ZIO.succeed(())
    yield ()

  def apply(outbox: Queue[Tx]): ZIO[Clock with Db.Db, Nothing, Unit] =
    catchAndLog(unsafeAutoquote(outbox))
