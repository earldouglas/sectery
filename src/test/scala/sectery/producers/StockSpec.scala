package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test.TestClock
import zio.test._

object StockSpec extends DefaultRunnableSpec:

  private val http: ULayer[Http.Service] =
    ZLayer.succeed[Http.Service] {
      new Http.Service:
        def request(
            method: String,
            url: String,
            headers: Map[String, String],
            body: Option[String]
        ): UIO[Response] =
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

  override def spec =
    suite(getClass().getName())(
      test("@stock VOO produces quote") {
        for
          (inbox, outbox, _) <- MessageQueues.loop
            .inject_(TestDb(), http)
          _ <- inbox.offer(Rx("#foo", "bar", "@stock VOO"))
          _ <- inbox.offer(Rx("#foo", "bar", "@stock voo"))
          _ <- inbox.offer(Rx("#foo", "bar", "@stock FOO"))
          _ <- inbox.offer(Rx("#foo", "bar", "@stock VOO BAR"))
          _ <- inbox.offer(Rx("#foo", "bar", "@stock"))
          _ <- inbox.offer(Rx("#foo", "bar", "@set stock VOO BAR"))
          _ <- inbox.offer(Rx("#foo", "bar", "@stock"))
          _ <- TestClock.adjust(1.seconds)
          ms <- outbox.takeAll
        yield assert((ms))(
          equalTo(
            List(
              Tx("#foo", "VOO: 390.09 +0.68 (+0.17%)"),
              Tx("#foo", "VOO: 390.09 +0.68 (+0.17%)"),
              Tx("#foo", "FOO: stonk not found"),
              Tx("#foo", "VOO: 390.09 +0.68 (+0.17%)"),
              Tx("#foo", "BAR: 1.00 +0.00 (+0.00%)"),
              Tx(
                "#foo",
                "Set default symbols with `@set stock <symbol1> [symbol2] ... [symbolN]`"
              ),
              Tx("#foo", "bar: stock set to VOO BAR"),
              Tx("#foo", "VOO: 390.09 +0.68 (+0.17%)"),
              Tx("#foo", "BAR: 1.00 +0.00 (+0.00%)")
            )
          )
        )
      } @@ timeout(2.seconds)
    )
