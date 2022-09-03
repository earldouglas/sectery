package sectery.producers

import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale
import scala.collection.JavaConverters._
import sectery.Http
import sectery.Producer
import sectery.Response
import sectery.Rx
import sectery.Tx
import zio.Clock
import zio.ZIO
import zio.json._

object CoindeskApi:

  case class USD(
      code: String,
      symbol: String,
      rate: String,
      description: String,
      rate_float: Float
  )

  object USD:
    implicit val decoder: JsonDecoder[USD] =
      DeriveJsonDecoder.gen[USD]

  case class BPI(USD: USD)

  object BPI:
    implicit val decoder: JsonDecoder[BPI] =
      DeriveJsonDecoder.gen[BPI]

  case class CurrentPrice(bpi: BPI)

  object CurrentPrice:
    implicit val decoder: JsonDecoder[CurrentPrice] =
      DeriveJsonDecoder.gen[CurrentPrice]

object Btc extends Producer:

  private val findRate: ZIO[Http.Http, Throwable, Option[Float]] =
    Http
      .request(
        method = "GET",
        url = "https://api.coindesk.com/v1/bpi/currentprice.json",
        headers = Map("User-Agent" -> "bot"),
        body = None
      )
      .flatMap { case Response(200, _, body) =>
        body.fromJson[CoindeskApi.CurrentPrice] match
          case Right(cp) =>
            ZIO.succeed(Some(cp.bpi.USD.rate_float))
          case Left(_) =>
            ZIO.succeed(None)
      }

  override def help(): Iterable[Info] =
    Some(Info("@btc", "@btc"))

  override def apply(m: Rx): ZIO[Http.Http, Throwable, Iterable[Tx]] =
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
