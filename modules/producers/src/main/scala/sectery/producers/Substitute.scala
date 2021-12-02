package sectery.producers

import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import scala.util.matching.Regex
import sectery.Db
import sectery.Producer
import sectery.Response
import sectery.Rx
import sectery.Tx
import zio.Clock
import zio.ZIO

object Substitute extends Producer:

  val subFirst = """s\/(.*)\/(.*)\/$""".r
  val subAll = """s\/(.*)\/(.*)\/g""".r

  override def help(): Iterable[Info] =
    Some(Info("s///", "s/find/replace/"))

  private def substitute(
      channel: String,
      toReplace: String,
      howReplace: String => String
  ): ZIO[Db.Db with Clock, Throwable, Iterable[Tx]] =
    val matcher: Regex = new Regex(s".*${toReplace}.*")
    Db { conn =>
      val ms = LastMessage.lastMessages(channel)(conn)
      ms.find(m => matcher.matches(m.message)) map { m =>
        val replacedMsg: String = howReplace(m.message)
        Tx(channel, s"<${m.nick}> ${replacedMsg}")
      }
    }

  override def apply(
      m: Rx
  ): ZIO[Db.Db with Clock, Throwable, Iterable[Tx]] =
    m match
      case Rx(channel, nick, subFirst(toReplace, withReplace)) =>
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
