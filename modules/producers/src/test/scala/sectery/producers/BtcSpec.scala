package sectery.producers

import sectery._
import sectery._
import zio._

object BtcSpec extends ProducerSpec:

  override def http =
    ZLayer.succeed[Http.Service] {
      new Http.Service:
        def request(
            method: String,
            url: String,
            headers: Map[String, String],
            body: Option[String]
        ): ZIO[Any, Nothing, Response] =
          url match
            case "https://api.coindesk.com/v1/bpi/currentprice.json" =>
              ZIO.succeed {
                Response(
                  status = 200,
                  headers = Map.empty,
                  body = Resource.read(
                    "/com/coindesk/api/v1/bpi/currentprice.json"
                  )
                )
              }
    }

  override val specs =
    Map(
      "@btc produces rate" ->
        (
          List(Rx("#foo", "bar", "@btc")),
          List(Tx("#foo", "$39,193.03"))
        )
    )
