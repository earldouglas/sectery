package sectery.producers

import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
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
import zio.URIO
import zio.ZIO

object Substitute extends Producer:

  type Channel = String
  type Nick = String
  type Msg = String

  private val sub = """s\/(.*)\/(.*)\/""".r

  override def help(): Iterable[Info] =
    Some(Info("s///", "s/find/replace/"))

  override def apply(m: Rx): URIO[Db.Db with Has[Clock], Iterable[Tx]] =
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
            ZIO.succeed(None)
          }
          .map(_.toIterable)
      case _ =>
        ZIO.succeed(None)
