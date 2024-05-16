package sectery.adaptors

import java.net.URI
import sectery._
import sectery.control.Monad
import sectery.control.Monad._
import sectery.effects.HttpClient.Response
import sectery.effects._
import zio.json._

object LiveBtc:

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

  def apply[F[_]: HttpClient: Monad](): Btc[F] =
    new Btc:
      override def toUsd(): F[Option[Float]] =
        summon[HttpClient[F]]
          .request(
            method = "GET",
            url = new URI(
              "https://api.coindesk.com/v1/bpi/currentprice.json"
            ).toURL(),
            headers = Map("User-Agent" -> "bot"),
            body = None
          )
          .map { case Response(200, _, body) =>
            body.fromJson[CoindeskApi.CurrentPrice] match
              case Right(cp) =>
                Some(cp.bpi.USD.rate_float)
              case Left(_) =>
                None
          }
