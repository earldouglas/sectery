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

object Substitute extends Producer:

  type Channel = String
  type Nick = String
  type Msg = String

  private val sub = """s\/(.*)\/(.*)\/""".r

  override def help(): Iterable[Info] =
    Some(Info("s///", "s/find/replace/"))

  override def apply(m: Rx): RIO[Db.Db with Has[Clock], Iterable[Tx]] =
    m match
      case Rx(channel, nick, sub(toReplace, withReplace)) =>
        val matcher: Regex = new Regex(s".*${toReplace}.*")
        Db { conn =>
          val ms = LastMessage.lastMessages(channel)(conn)
          ms.find(m => matcher.matches(m.message)) map { m =>
            val replacedMsg: Msg =
              m.message.replaceAll(toReplace, withReplace)
            Tx(channel, s"<${m.nick}> ${replacedMsg}")
          }
        }
      case _ =>
        ZIO.succeed(None)
