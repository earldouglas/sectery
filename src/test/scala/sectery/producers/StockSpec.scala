package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.duration._
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
            case "https://finnhub.io/api/v1/quote?symbol=FOO&token=alligator3" =>
              ZIO.effectTotal {
                Response(
                  status = 200,
                  headers = Map.empty,
                  body = """|{
                            |  "c": 6,
                            |  "h": 7,
                            |  "l": 4,
                            |  "o": 5,
                            |  "pc": 4,
                            |  "t": 1623096002
                            |}
                            |""".stripMargin
                )
              }
            case _ =>
              ZIO.effectTotal {
                Response(
                  status = 200,
                  headers = Map.empty,
                  body = """|{
                            |  "c": 0,
                            |  "h": 0,
                            |  "l": 0,
                            |  "o": 0,
                            |  "pc": 0,
                            |  "t": 1623096002
                            |}
                            |""".stripMargin
                )
              }
    }

  override def spec =
    suite(getClass().getName())(
      testM("@stock FOO produces quote") {
        for
          sent <- ZQueue.unbounded[Tx]
          inbox <- MessageQueues
            .loop(new MessageLogger(sent))
            .inject(TestDb(), http)
          _ <- inbox.offer(Rx("#foo", "bar", "@stock FOO"))
          _ <- inbox.offer(Rx("#foo", "bar", "@stock foo"))
          _ <- inbox.offer(Rx("#foo", "bar", "@stock BAR"))
          _ <- TestClock.adjust(1.seconds)
          m1 <- sent.take
          m2 <- sent.take
          m3 <- sent.take
        yield assert((m1, m2, m3))(
          equalTo(
            (
              Tx("#foo", "FOO: 6.00 +2.00 (+50.00%)"),
              Tx("#foo", "foo: 6.00 +2.00 (+50.00%)"),
              Tx("#foo", "BAR: stonk not found")
            )
          )
        )
      } @@ timeout(2.seconds)
    )
