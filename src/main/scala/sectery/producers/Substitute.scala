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

  private val subOne = """s\/(.*)\/(.*)\/$""".r
  private val subAll = """s\/(.*)\/(.*)\/g""".r

  override def help(): Iterable[Info] =
    Some(Info("s///", "s/find/replace/"))

  private def substitute(
      channel: String,
      toReplace: String,
      howReplace: String => String
  ): RIO[Db.Db with Has[Clock], Iterable[Tx]] =
    val matcher: Regex = new Regex(s".*${toReplace}.*")
    Db { conn =>
      val ms = LastMessage.lastMessages(channel)(conn)
      ms.find(m => matcher.matches(m.message)) map { m =>
        val replacedMsg: String = howReplace(m.message)
        Tx(channel, s"<${m.nick}> ${replacedMsg}")
      }
    }

  override def apply(m: Rx): RIO[Db.Db with Has[Clock], Iterable[Tx]] =
    m match
      case Rx(channel, nick, subOne(toReplace, withReplace)) =>
        substitute(
          channel,
          toReplace,
          _.replaceFirst(toReplace, withReplace)
        )
      case Rx(channel, nick, subAll(toReplace, withReplace)) =>
        substitute(
          channel,
          toReplace,
          _.replaceAll(toReplace, withReplace)
        )
      case _ =>
        ZIO.succeed(None)
