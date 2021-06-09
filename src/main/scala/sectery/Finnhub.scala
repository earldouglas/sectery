package sectery

import org.json4s.JsonDSL._
import org.json4s.MonadicJValue.jvalueToMonadic
import org.json4s._
import org.json4s.native.JsonMethods._
import org.slf4j.LoggerFactory
import scala.util.Try
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

  def floatish(v: JValue): Option[Float] =
    v match
      case JDouble(x)  => Some(x.toFloat)
      case JDecimal(x) => Some(x.toFloat)
      case JInt(x)     => Some(x.toFloat)
      case JLong(x)    => Some(x.toFloat)
      case _           => None

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
          val quote: Option[Quote] =
            for
              json <- Try(parse(body)).toOption
              c <- floatish(json \ "c")
              h <- floatish(json \ "h")
              l <- floatish(json \ "l")
              o <- floatish(json \ "o")
              pc <- floatish(json \ "pc")
              // If Finnhub can't find a symbol, it returns all zeroes
              if c != 0 || h != 0 || l != 0 || o != 0 || pc != 0
            yield Quote(
              open = o.toFloat,
              high = h.toFloat,
              low = l.toFloat,
              current = c.toFloat,
              previousClose = pc.toFloat
            )
          ZIO.effect(quote)
        case _ =>
          ZIO.effectTotal(None)
      }
      .catchAll { e =>
        LoggerFactory
          .getLogger(this.getClass())
          .error("caught exception", e)
        ZIO.effectTotal(None)
      }
