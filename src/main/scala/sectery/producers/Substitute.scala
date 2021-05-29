package sectery.producers

import scala.collection.concurrent.{TrieMap => Map}
import scala.util.matching.Regex
import sectery.Producer
import sectery.Response
import sectery.Rx
import sectery.Tx
import zio.clock.Clock
import zio.URIO
import zio.ZIO

object Substitute extends Producer:

  type Channel = String
  type Nick = String
  type Msg = String
  val sub = """s\/(.*)\/(.*)\/""".r
  val mMap: Map[Channel, Map[Nick, Msg]]= Map()

  def apply(m: Rx): URIO[Clock, Iterable[Tx]] =
    m match
      case Rx(channel, nick, sub(toReplace, withReplace)) =>
        val matcher: Regex = new Regex(s".*${toReplace}.*")
        val msgs: Option[(Nick, Msg)] =
          mMap
            .get(channel)
            .flatMap {
              _.find { case (nick, msg) =>
                matcher.matches(msg)
              }
            }
        val txs: Option[Tx] =
          msgs.map { case (nick, msg) =>
            val replacedMsg: Msg =
              msg.replaceAll(toReplace, withReplace)
            Tx(channel, s"<${nick}> ${replacedMsg}")
          }
        ZIO.effectTotal(txs)
      case Rx(channel, nick, msg) =>
        val msgs = mMap.getOrElse(channel, Map[Nick, Msg]())
        msgs.update(nick, msg)
        mMap.update(channel, msgs)
        ZIO.effectTotal(None)
