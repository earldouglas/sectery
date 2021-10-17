package sectery.producers

import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import scala.util.matching.Regex
import sectery.Db
import sectery.Info
import sectery.Producer
import sectery.Response
import sectery.Rx
import sectery.Tx
import zio.Clock
import zio.Has
import zio.RIO
import zio.ZIO

case class LastMessage(
    channel: String,
    nick: String,
    message: String,
    timestamp: Timestamp
)

object LastMessage extends Producer:

  override def help(): Iterable[Info] =
    None

  override def init(): RIO[Db.Db, Unit] =
    for
      _ <- Db.query { conn =>
        val s =
          """|CREATE TABLE IF NOT EXISTS
             |LAST_MESSAGE(
             |  CHANNEL VARCHAR(256) NOT NULL,
             |  NICK VARCHAR(256) NOT NULL,
             |  MESSAGE TEXT NOT NULL,
             |  TIMESTAMP TIMESTAMP NOT NULL,
             |  PRIMARY KEY (CHANNEL, NICK)
             |)
             |""".stripMargin
        val stmt = conn.createStatement
        stmt.executeUpdate(s)
        stmt.close()
      }
      _ <- Db.query { conn =>
        val s =
          """|CREATE TABLE IF NOT EXISTS
             |AUTOQUOTE(
             |  CHANNEL VARCHAR(256) NOT NULL,
             |  TIMESTAMP TIMESTAMP NOT NULL,
             |  PRIMARY KEY (CHANNEL)
             |)
             |""".stripMargin
        val stmt = conn.createStatement
        stmt.executeUpdate(s)
        stmt.close()
      }
    yield ()

  override def apply(m: Rx): RIO[Db.Db with Has[Clock], Iterable[Tx]] =
    for
      _ <- Db.query { conn =>
        val s =
          "DELETE FROM LAST_MESSAGE WHERE CHANNEL = ? AND NICK = ?"
        val stmt = conn.prepareStatement(s)
        stmt.setString(1, m.channel)
        stmt.setString(2, m.nick)
        stmt.executeUpdate
        stmt.close()
      }
      nowMillis <- Clock.currentTime(TimeUnit.MILLISECONDS)
      newCount <- Db.query { conn =>
        val s =
          "INSERT INTO LAST_MESSAGE (CHANNEL, NICK, MESSAGE, TIMESTAMP) VALUES (?, ?, ?, ?)"
        val stmt = conn.prepareStatement(s)
        stmt.setString(1, m.channel)
        stmt.setString(2, m.nick)
        stmt.setString(3, m.message)
        stmt.setTimestamp(4, new Timestamp(nowMillis))
        stmt.executeUpdate
        stmt.close()
      }
      _ <- Db.query { conn =>
        val s =
          "DELETE FROM AUTOQUOTE WHERE CHANNEL = ?"
        val stmt = conn.prepareStatement(s)
        stmt.setString(1, m.channel)
        stmt.executeUpdate
        stmt.close()
      }
      nowMillis <- Clock.currentTime(TimeUnit.MILLISECONDS)
      newCount <- Db.query { conn =>
        val s =
          "INSERT INTO AUTOQUOTE (CHANNEL, TIMESTAMP) VALUES (?, ?)"
        val stmt = conn.prepareStatement(s)
        stmt.setString(1, m.channel)
        stmt.setTimestamp(2, new Timestamp(nowMillis))
        stmt.executeUpdate
        stmt.close()
      }
    yield None

  def lastMessages(channel: String): RIO[Db.Db, List[LastMessage]] =
    Db.query { conn =>
      val s =
        """|SELECT NICK, MESSAGE, TIMESTAMP
           |FROM LAST_MESSAGE
           |WHERE CHANNEL = ?
           |ORDER BY TIMESTAMP DESC
           |""".stripMargin
      val stmt = conn.prepareStatement(s)
      stmt.setString(1, channel)
      val rs = stmt.executeQuery
      var ms: List[LastMessage] = Nil
      while (rs.next()) {
        val nick = rs.getString("NICK")
        val message = rs.getString("MESSAGE")
        val timestamp = rs.getTimestamp("TIMESTAMP")
        ms = LastMessage(
          channel = channel,
          nick = nick,
          message = message,
          timestamp = timestamp
        ) :: ms
      }
      stmt.close()
      ms.reverse
    }

  def lastMessage(
      channel: String,
      nick: String
  ): RIO[Db.Db, Option[LastMessage]] =
    Db.query { conn =>
      val s =
        """|SELECT MESSAGE, TIMESTAMP
           |FROM LAST_MESSAGE
           |WHERE CHANNEL = ?
           |  AND NICK = ?
           |ORDER BY TIMESTAMP DESC
           |""".stripMargin
      val stmt = conn.prepareStatement(s)
      stmt.setString(1, channel)
      stmt.setString(2, nick)
      val rs = stmt.executeQuery
      var mo: Option[LastMessage] = None
      if (rs.next()) {
        val message = rs.getString("MESSAGE")
        val timestamp = rs.getTimestamp("TIMESTAMP")
        mo = Some(
          LastMessage(
            channel = channel,
            nick = nick,
            message = message,
            timestamp = timestamp
          )
        )
      }
      stmt.close()
      mo
    }
