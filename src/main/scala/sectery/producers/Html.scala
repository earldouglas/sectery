package sectery.producers

import java.net.URLEncoder
import sectery.Http
import sectery.Producer
import sectery.Response
import sectery.Rx
import sectery.Tx
import zio.clock.Clock
import zio.Has
import zio.URIO
import zio.ZIO

object Html extends Producer:

  val url = """.*(http[^\s]+).*""".r

  val desc = """.*<\s*meta\s+name="description"\s+content="([^"]+)"\s*>.*""".r
  val ogDesc1 = """.*<\s*meta\s+property="og:description"\s+content="([^"]+)"\s*>.*""".r
  val ogDesc2 = """.*<\s*meta\s+name="og:description"\s+content="([^"]+)"\s*>.*""".r
  val twitDesc1 = """.*<\s*meta\s+property="twitter:description"\s+content="([^"]+)"\s*>.*""".r
  val twitDesc2 = """.*<\s*meta\s+name="twitter:description"\s+content="([^"]+)"\s*>.*""".r

  val title = """.*<\s*title>\s*(.+)\s*<\/title>.*""".r
  val ogTitle1 = """.*<\s*meta\s+property="og:title"\s+content="([^"]+)"\s*>.*""".r
  val ogTitle2 = """.*<\s*meta\s+name="og:title"\s+content="([^"]+)"\s*>.*""".r
  val twitTitle1 = """.*<\s*meta\s+property="twitter:title"\s+content="([^"]+)"\s*>.*""".r
  val twitTitle2 = """.*<\s*meta\s+name="twitter:title"\s+content="([^"]+)"\s*>.*""".r

  def apply(m: Rx): URIO[Http.Http, Iterable[Tx]] =
    m match
      case Rx(c, _, url(url)) =>
        Http.request(
          method = "GET",
          url = url,
          headers = Map("User-Agent" -> "bot"),
          body = None
        ).map {
          case Response(200, _, body) =>
            body
              .replaceAll("[\\r\\n]", " ")
              .replaceAll("\\s+", " ") match
              case desc(d) => Some(Tx(c, d))
              case ogDesc1(d) => Some(Tx(c, d))
              case ogDesc2(d) => Some(Tx(c, d))
              case twitDesc1(d) => Some(Tx(c, d))
              case twitDesc2(d) => Some(Tx(c, d))
              case title(t) => Some(Tx(c, t))
              case ogTitle1(t) => Some(Tx(c, t))
              case ogTitle2(t) => Some(Tx(c, t))
              case twitTitle1(t) => Some(Tx(c, t))
              case twitTitle2(t) => Some(Tx(c, t))
              case _ => None
          case r =>
            None
        }.catchAll { e =>
            ZIO.effectTotal(None)
        }.map(_.toIterable)
      case _ =>
        ZIO.effectTotal(None)
