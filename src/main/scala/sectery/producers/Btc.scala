package sectery.producers

import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.slf4j.LoggerFactory
import scala.collection.JavaConverters._
import sectery.Http
import sectery.Info
import sectery.Producer
import sectery.Response
import sectery.Rx
import sectery.Tx
import zio.Clock
import zio.Has
import zio.RIO
import zio.ZIO

object Btc extends Producer:

  private val findRate: RIO[Http.Http, Option[Double]] =
    Http
      .request(
        method = "GET",
        url = "https://api.coindesk.com/v1/bpi/currentprice.json",
        headers = Map("User-Agent" -> "bot"),
        body = None
      )
      .flatMap { case Response(200, _, body) =>
        ZIO.attempt {
          val json = parse(body)
          json \ "bpi" \ "USD" \ "rate_float" match
            case JDouble(rate) =>
              Some(rate)
            case r =>
              LoggerFactory
                .getLogger(this.getClass())
                .error(s"unexpected response: ${r}")
              None
        }
      }

  override def help(): Iterable[Info] =
    Some(Info("@btc", "@btc"))

  override def apply(m: Rx): RIO[Http.Http, Iterable[Tx]] =
    m match
      case Rx(c, _, "@btc") =>
        findRate.flatMap {
          case Some(rate) =>
            ZIO.succeed(
              Some(
                Tx(
                  c,
                  s"${NumberFormat.getCurrencyInstance(Locale.US).format(rate)}"
                )
              )
            )
          case None =>
            ZIO.succeed(None)
        }
      case _ =>
        ZIO.succeed(None)
