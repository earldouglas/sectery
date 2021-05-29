package sectery.producers

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import scala.collection.JavaConverters._
import scala.collection.concurrent.{TrieMap => Map}
import sectery.Http
import sectery.Producer
import sectery.Response
import sectery.Rx
import sectery.Tx
import zio.clock.Clock
import zio.Has
import zio.URIO
import zio.ZIO
import zio.Ref

object Substitute extends Producer:

  type Channel = String
  type Nick = String
  type Msg = String
  val sub = """s\/(.*)\/(.*)\/""".r
  val mMap: Map[Channel, Map[Nick, Msg]]= Map()

  def apply(m: Rx): URIO[Clock, Iterable[Tx]] =
    m match
      case Rx(channel, nick, sub(toReplace, withReplace)) =>
        val msg = mMap.get(channel).flatMap(_.get(nick))
        val replaced = msg.map(_.replaceAll(toReplace, withReplace))
        val tx = replaced.map(m => Tx(channel, m))
        ZIO.effectTotal(tx)
      case Rx(channel, nick, msg) =>
        mMap.getOrElse(channel, Map[Nick, Msg]()).update(nick, msg)
        ZIO.effectTotal(None)
