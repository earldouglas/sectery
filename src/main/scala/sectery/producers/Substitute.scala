package sectery.producers

import java.sql.{Date => SqlDate}
import java.util.Date
import org.slf4j.LoggerFactory
import scala.util.matching.Regex
import sectery.Db
import sectery.Info
import sectery.Producer
import sectery.Response
import sectery.Rx
import sectery.Tx
import zio.RIO
import zio.URIO
import zio.ZIO

object Substitute extends Producer:

  type Channel = String
  type Nick = String
  type Msg = String

  private val sub = """s\/(.*)\/(.*)\/""".r

  override def help(): Iterable[Info] =
    Some(Info("s///", "s/find/replace/"))

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
        stmt.close
      }
    yield ()

  override def apply(m: Rx): URIO[Db.Db, Iterable[Tx]] =
    m match
      case Rx(channel, nick, sub(toReplace, withReplace)) =>
        val matcher: Regex = new Regex(s".*${toReplace}.*")
        val sub =
          for
            m <- Db.query { conn =>
              val s =
                """|SELECT NICK, MESSAGE
                   |FROM LAST_MESSAGE
                   |WHERE CHANNEL = ?
                   |ORDER BY TIMESTAMP DESC
                   |""".stripMargin
              val stmt = conn.prepareStatement(s)
              stmt.setString(1, channel)
              val rs = stmt.executeQuery
              var m: Option[(Nick, Msg)] = None
              while (m.isEmpty && rs.next()) {
                val nick = rs.getString("NICK")
                val msg = rs.getString("MESSAGE")
                if (matcher.matches(msg)) {
                  m = Some((nick, msg))
                }
              }
              stmt.close
              m
            }
          yield m.map { case (nick, msg) =>
            val replacedMsg: Msg =
              msg.replaceAll(toReplace, withReplace)
            Tx(channel, s"<${nick}> ${replacedMsg}")
          }
        sub
          .catchAll { e =>
            LoggerFactory
              .getLogger(this.getClass())
              .error("caught exception", e)
            ZIO.effectTotal(None)
          }
          .map(_.toIterable)
      case Rx(channel, nick, msg) =>
        val increment =
          for
            _ <- Db.query { conn =>
              val s =
                "DELETE FROM LAST_MESSAGE WHERE CHANNEL = ? AND NICK = ?"
              val stmt = conn.prepareStatement(s)
              stmt.setString(1, channel)
              stmt.setString(2, nick)
              stmt.executeUpdate
              stmt.close
            }
            newCount <- Db.query { conn =>
              val s =
                "INSERT INTO LAST_MESSAGE (CHANNEL, NICK, MESSAGE, TIMESTAMP) VALUES (?, ?, ?, ?)"
              val stmt = conn.prepareStatement(s)
              stmt.setString(1, channel)
              stmt.setString(2, nick)
              stmt.setString(3, msg)
              stmt.setDate(4, new SqlDate((new Date()).getTime()))
              stmt.executeUpdate
              stmt.close
            }
          yield None
        increment
          .catchAll { e =>
            LoggerFactory
              .getLogger(this.getClass())
              .error("caught exception", e)
            ZIO.effectTotal(None)
          }
          .map(_.toIterable)
