package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.duration._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test._
import zio.test.environment.TestClock

object BtcSpec extends DefaultRunnableSpec:

  val http: ULayer[Has[Http.Service]] =
    ZLayer.succeed {
      new Http.Service:
        def request(
            method: String,
            url: String,
            headers: Map[String, String],
            body: Option[String]
        ): UIO[Response] =
          url match
            case "https://api.coindesk.com/v1/bpi/currentprice.json" =>
              ZIO.effectTotal {
                Response(
                  status = 200,
                  headers = Map.empty,
                  body = """|{
                            |  "time": {
                            |  },
                            |  "disclaimer": "",
                            |  "chartName": "Bitcoin",
                            |  "bpi": {
                            |    "USD": {
                            |      "code": "USD",
                            |      "symbol": "&#36;",
                            |      "rate": "35,929.0133",
                            |      "description": "United States Dollar",
                            |      "rate_float": 35929.0133
                            |    },
                            |    "GBP": {
                            |    },
                            |    "EUR": {
                            |    }
                            |  }
                            |}
                            |""".stripMargin
                )
              }
    }

  override def spec =
    suite(getClass().getName())(
      testM("@btc produces rate") {
        for
          sent <- ZQueue.unbounded[Tx]
          inbox <- MessageQueues
            .loop(new MessageLogger(sent))
            .inject(TestFinnhub(), TestDb(), http)
          _ <- inbox.offer(Rx("#foo", "bar", "@btc"))
          _ <- TestClock.adjust(1.seconds)
          ms <- sent.takeAll
        yield assert(ms)(equalTo(List(Tx("#foo", "$35,929.01"))))
      } @@ timeout(2.seconds)
    )
