package sectery

import java.sql.Timestamp
import java.util.Calendar
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import sectery.Db
import sectery.Tx
import zio.Clock
import zio.Has
import zio.Queue
import zio.RIO
import zio.URIO
import zio.ZIO

object Autoquote:

  private def timeForAutoquote(nowMillis: Long): Boolean =

    val eightToFive: Boolean =
      val hourOfDay =
        val c = Calendar.getInstance()
        c.setTimeInMillis(nowMillis)
        c.get(Calendar.HOUR_OF_DAY)
      hourOfDay >= 8 && hourOfDay < 17

    val mondayThroughFriday: Boolean =
      val dayOfWeek =
        val c = Calendar.getInstance()
        c.setTimeInMillis(nowMillis)
        c.get(Calendar.DAY_OF_WEEK)
      dayOfWeek == Calendar.MONDAY ||
      dayOfWeek == Calendar.TUESDAY ||
      dayOfWeek == Calendar.WEDNESDAY ||
      dayOfWeek == Calendar.THURSDAY ||
      dayOfWeek == Calendar.FRIDAY

    eightToFive && mondayThroughFriday

  private def getAutoquoteChannels(
      nowMillis: Long
  ): RIO[Db.Db, List[String]] =
    for
      channels <-
        Db.query { conn =>
          val s =
            """|SELECT CHANNEL
               |FROM AUTOQUOTE
               |WHERE TIMESTAMP <= ?
               |""".stripMargin
          val stmt = conn.prepareStatement(s)
          val hourAgoMillis = nowMillis - 60L * 60L * 1000L
          stmt.setTimestamp(1, new Timestamp(hourAgoMillis))
          val rs = stmt.executeQuery
          var cs: List[String] = Nil
          if (rs.next()) {
            val channel = rs.getString("CHANNEL")
            cs = channel :: cs
          }
          stmt.close()
          cs
        }
      _ <-
        Db.query { conn =>
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
        }
    yield channels

  private def getAutoquoteMessages(
      channels: List[String]
  ): RIO[Db.Db, List[Tx]] =
    ZIO
      .collectAll(
        channels.map { channel =>
          Db.query { conn =>
            val s =
              """|SELECT NICK, MESSAGE
                 |FROM GRABBED_MESSAGES
                 |WHERE CHANNEL = ?
                 |ORDER BY RANDOM()
                 |LIMIT 1
                 |""".stripMargin
            val stmt = conn.prepareStatement(s)
            stmt.setString(1, channel)
            val rs = stmt.executeQuery
            var txo: Option[Tx] = None
            if (rs.next()) {
              val message = rs.getString("MESSAGE")
              val nick = rs.getString("NICK")
              txo = Some(
                Tx(
                  channel = channel,
                  message = s"<${nick}> ${message}"
                )
              )
            }
            stmt.close
            txo
          }
        }
      )
      .map(_.flatten)

  private def unsafeAutoquote(
      outbox: Queue[Tx]
  ): RIO[Has[Clock] with Db.Db, Unit] =
    for
      nowMillis <- Clock.currentTime(TimeUnit.MILLISECONDS)
      _ <-
        if (timeForAutoquote(nowMillis))
          for
            channels <- getAutoquoteChannels(nowMillis)
            messages <- getAutoquoteMessages(channels)
            _ <- ZIO.collectAll(messages.map(outbox.offer))
          yield ()
        else
          ZIO.succeed(())
    yield ()

  def apply(outbox: Queue[Tx]): URIO[Has[Clock] with Db.Db, Unit] =
    unsafeAutoquote(outbox)
      .catchAllCause { cause =>
        LoggerFactory
          .getLogger(this.getClass())
          .error(cause.prettyPrint)
        ZIO.succeed(())
      }
