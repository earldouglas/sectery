package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test._
import zio.test.environment.TestClock

object StockSpec extends DefaultRunnableSpec:

  private val http: ULayer[Has[Http.Service]] =
    ZLayer.succeed {
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
    }

  override def spec =
    suite(getClass().getName())(
      test("@stock VOO produces quote") {
        for
          sent <- ZQueue.unbounded[Tx]
          (inbox, _) <- MessageQueues
            .loop(new MessageLogger(sent))
            .inject_(TestDb(), http)
          _ <- inbox.offer(Rx("#foo", "bar", "@stock VOO"))
          _ <- inbox.offer(Rx("#foo", "bar", "@stock voo"))
          _ <- inbox.offer(Rx("#foo", "bar", "@stock FOO"))
          _ <- TestClock.adjust(1.seconds)
          m1 <- sent.take
          m2 <- sent.take
          m3 <- sent.take
        yield assert((m1, m2, m3))(
          equalTo(
            (
              Tx("#foo", "VOO: 390.09 +0.68 (+0.17%)"),
              Tx("#foo", "voo: 390.09 +0.68 (+0.17%)"),
              Tx("#foo", "FOO: stonk not found")
            )
          )
        )
      } @@ timeout(2.seconds)
    )
