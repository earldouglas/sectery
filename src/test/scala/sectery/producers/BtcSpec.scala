package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test.TestClock
import zio.test._

object BtcSpec extends DefaultRunnableSpec:

  val http: ULayer[Http.Service] =
    ZLayer.succeed[Http.Service] {
      new Http.Service:
        def request(
            method: String,
            url: String,
            headers: Map[String, String],
            body: Option[String]
        ): UIO[Response] =
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

  override def spec =
    suite(getClass().getName())(
      test("@btc produces rate") {
        for
          (inbox, outbox, _) <- MessageQueues.loop
            .inject_(TestDb(), http)
          _ <- inbox.offer(Rx("#foo", "bar", "@btc"))
          _ <- TestClock.adjust(1.seconds)
          ms <- outbox.takeAll
        yield assert(ms)(equalTo(List(Tx("#foo", "$39,193.03"))))
      } @@ timeout(2.seconds)
    )
