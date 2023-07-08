package sectery.producers

import sectery._
import zio._

object StockSpec extends ProducerSpec:

  override val http =
    ZLayer.succeed[Http.Service] {
      new Http.Service:
        def request(
            method: String,
            url: String,
            headers: Map[String, String],
            body: Option[String]
        ): ZIO[Any, Nothing, Response] =
          url match
            case "https://finnhub.io/api/v1/quote?symbol=VOO&token=alligator3" =>
              ZIO.succeed {
                Response(
                  status = 200,
                  headers = Map.empty,
                  body = Resource.read(
                    "/io/finnhub/api/v1/quote?symbol=VOO&token=alligator3"
                  )
                )
              }
            case "https://finnhub.io/api/v1/quote?symbol=FOO&token=alligator3" =>
              ZIO.succeed {
                Response(
                  status = 200,
                  headers = Map.empty,
                  body = Resource.read(
                    "/io/finnhub/api/v1/quote?symbol=FOO&token=alligator3"
                  )
                )
              }
            case "https://finnhub.io/api/v1/quote?symbol=BAR&token=alligator3" =>
              ZIO.succeed {
                Response(
                  status = 200,
                  headers = Map.empty,
                  body = Resource.read(
                    "/io/finnhub/api/v1/quote?symbol=BAR&token=alligator3"
                  )
                )
              }
    }

  override val specs =
    Map(
      "@stock VOO produces quote" ->
        (
          List(
            Rx("#foo", "bar", "@stock VOO"),
            Rx("#foo", "bar", "@stock voo"),
            Rx("#foo", "bar", "@stock FOO"),
            Rx("#foo", "bar", "@stock VOO BAR"),
            Rx("#foo", "bar", "@stock"),
            Rx("#foo", "bar", "@set stock VOO BAR"),
            Rx("#foo", "bar", "@stock")
          ),
          List(
            Tx("#foo", "VOO: 390.09 +0.68 (+0.17%)"),
            Tx("#foo", "VOO: 390.09 +0.68 (+0.17%)"),
            Tx("#foo", "FOO: stonk not found"),
            Tx("#foo", "VOO: 390.09 +0.68 (+0.17%)"),
            Tx("#foo", "BAR: 1.00 +0.00 (+0.00%)"),
            Tx(
              "#foo",
              "bar: Set default symbols with `@set stock <symbol1> [symbol2] ... [symbolN]`"
            ),
            Tx("#foo", "bar: stock set to VOO BAR"),
            Tx("#foo", "VOO: 390.09 +0.68 (+0.17%)"),
            Tx("#foo", "BAR: 1.00 +0.00 (+0.00%)")
          )
        )
    )
