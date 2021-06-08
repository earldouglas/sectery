package sectery

import org.slf4j.LoggerFactory
import sectery.Http
import zio.Has
import zio.Task
import zio.ULayer
import zio.URIO
import zio.ZIO
import zio.ZLayer

class Finnhub(apiToken: String):

  case class Quote(
      open: Float,
      high: Float,
      low: Float,
      current: Float,
      previousClose: Float
  )

  def quote(symbol: String): URIO[Http.Http, Option[Quote]] =
    Http
      .request(
        method = "GET",
        url =
          s"""https://finnhub.io/api/v1/quote?symbol=${symbol}&token=${apiToken}""",
        headers = Map("User-Agent" -> "bot"),
        body = None
      )
      .flatMap {
        case Response(200, _, body) =>
          // TODO this is awful, but I can't find a JSON parser that supports Scala 3
          val fields: Map[String, String] =
            body
              .replaceAll("""["{}\s\n\r]""", "")
              .split(",")
              .map(_.split(":"))
              .map(xs => xs(0) -> xs(1))
              .toMap
          val quote: Option[Quote] =
            for
              o <- fields.get("o").map(_.toFloat)
              h <- fields.get("h").map(_.toFloat)
              l <- fields.get("l").map(_.toFloat)
              c <- fields.get("c").map(_.toFloat)
              pc <- fields.get("pc").map(_.toFloat)
              q <- (o, h, l, c, pc) match
                case (0, 0, 0, 0, 0) =>
                  None
                case _ =>
                  Some(
                    Quote(
                      open = o,
                      high = h,
                      low = l,
                      current = c,
                      previousClose = pc
                    )
                  )
            yield q
          ZIO.effect(quote)
        case _ =>
          ZIO.effect(None)
      }
      .catchAll { e =>
        LoggerFactory
          .getLogger(this.getClass())
          .error("caught exception", e)
        ZIO.effectTotal(None)
      }
