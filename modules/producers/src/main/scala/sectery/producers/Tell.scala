package sectery.producers

import java.sql.Connection
import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import org.ocpsoft.prettytime.PrettyTime
import sectery.Db
import sectery.Producer
import sectery.Rx
import sectery.Tx
import zio.Clock
import zio.ZIO

object Tell extends Producer:

  private val tell = """^@tell\s+([^\s]+)\s+(.+)\s*$""".r

  override def help(): Iterable[Info] =
    Some(
      Info(
        "@tell",
        "@tell <nick> <message>, e.g. @tell bob remember the milk"
      )
    )

  override def init(): ZIO[Db.Db, Throwable, Unit] =
    Db { conn =>
      val s =
        """|CREATE TABLE IF NOT EXISTS TELL (
           |  _CHANNEL_ VARCHAR(64) NOT NULL,
           |  _FROM_ VARCHAR(64) NOT NULL,
           |  _TO_ VARCHAR(64) NOT NULL,
           |  _MESSAGE_ VARCHAR(64) NOT NULL,
           |  _TIMESTAMP_ TIMESTAMP NOT NULL
           |)
           |""".stripMargin
      val stmt = conn.createStatement
      stmt.executeUpdate(s)
      stmt.close
    }

  override def apply(
      m: Rx
  ): ZIO[Db.Db with Clock, Throwable, Iterable[Tx]] =
    m match
      case Rx(c, from, tell(to, message)) =>
        for {
          now <- Clock.currentTime(TimeUnit.MILLISECONDS)
          reply <- Db { conn =>
            val s =
              "INSERT INTO TELL (_CHANNEL_, _FROM_, _TO_, _MESSAGE_, _TIMESTAMP_) VALUES (?, ?, ?, ?, ?)"
            val stmt = conn.prepareStatement(s)
            stmt.setString(1, c)
            stmt.setString(2, from)
            stmt.setString(3, to)
            stmt.setString(4, message)
            stmt.setTimestamp(5, new Timestamp(now))
            stmt.executeUpdate()
            stmt.close
            Some(Tx(c, "I will let them know."))
          }
        } yield reply
      case Rx(c, nick, _) =>
        def findMessages(conn: Connection): List[Tx] =
          val s =
            """|SELECT _FROM_, _MESSAGE_, _TIMESTAMP_
               |FROM TELL
               |WHERE _CHANNEL_ = ? AND _TO_ = ?
               |ORDER BY _TIMESTAMP_ DESC
               |""".stripMargin
          val stmt = conn.prepareStatement(s)
          stmt.setString(1, c)
          stmt.setString(2, nick)
          val rs = stmt.executeQuery()
          var msgs: List[Tx] = Nil
          while (rs.next()) {
            val from = rs.getString("_FROM_")
            val message = rs.getString("_MESSAGE_")
            val timestamp = rs.getDate("_TIMESTAMP_")
            val prettytime = (new PrettyTime).format(timestamp)
            msgs = Tx(
              c,
              s"${nick}: ${prettytime}, ${from} said: ${message}"
            ) :: msgs
          }
          stmt.close
          msgs

        def dropMessages(conn: Connection): Unit =
          val s =
            "DELETE FROM TELL WHERE _CHANNEL_ = ? AND _TO_ = ?"
          val stmt = conn.prepareStatement(s)
          stmt.setString(1, c)
          stmt.setString(2, nick)
          stmt.executeUpdate()
          stmt.close

        Db { conn =>
          val messages = findMessages(conn)
          dropMessages(conn)
          messages
        }
