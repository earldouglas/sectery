package sectery.producers

import java.sql.Date
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import sectery.Db
import sectery.Info
import sectery.Producer
import sectery.Rx
import sectery.Tx
import zio.Clock
import zio.Has
import zio.RIO
import zio.UIO
import zio.URIO
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

  override def init(): RIO[Db.Db, Unit] =
    for
      _ <- Db.query { conn =>
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
    yield ()

  override def apply(m: Rx): URIO[Db.Db with Has[Clock], Iterable[Tx]] =
    m match
      case Rx(c, from, tell(to, message)) =>
        for {
          now <- Clock.currentTime(TimeUnit.MILLISECONDS)
          reply <- Db
            .query { conn =>
              val s =
                "INSERT INTO TELL (_CHANNEL_, _FROM_, _TO_, _MESSAGE_, _TIMESTAMP_) VALUES (?, ?, ?, ?, ?)"
              val stmt = conn.prepareStatement(s)
              stmt.setString(1, c)
              stmt.setString(2, from)
              stmt.setString(3, to)
              stmt.setString(4, message)
              stmt.setDate(5, new Date(now))
              stmt.executeUpdate()
              stmt.close
              Some(Tx(c, "I will let them know."))
            }
            .catchAll { e =>
              LoggerFactory
                .getLogger(this.getClass())
                .error("caught exception", e)
              ZIO.succeed(None)
            }
        } yield reply
      case Rx(c, nick, _) =>
        val increment =
          for
            messages <- Db.query { conn =>
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
                msgs = Tx(
                  c,
                  s"${nick}: on ${timestamp}, ${from} said: ${message}"
                ) :: msgs
              }
              stmt.close
              msgs
            }
            _ <- Db.query { conn =>
              val s =
                "DELETE FROM TELL WHERE _CHANNEL_ = ? AND _TO_ = ?"
              val stmt = conn.prepareStatement(s)
              stmt.setString(1, c)
              stmt.setString(2, nick)
              stmt.executeUpdate()
              stmt.close
            }
          yield messages
        increment
          .catchAll { e =>
            LoggerFactory
              .getLogger(this.getClass())
              .error("caught exception", e)
            ZIO.succeed(None)
          }
