package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.duration._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test._
import zio.test.environment.TestClock

object ZillowSpec extends DefaultRunnableSpec:
  override def spec =
    suite(getClass().getName())(
      testM("@zillow produces zestimate") {
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
                  case "https://www.zillow.com/homes/123-main-st-cityville-st_rb/" =>
                    ZIO.effectTotal {
                      Response(
                        status = 200,
                        headers = Map.empty,
                        body =
                          """<input id="Est.-selling-price-of-your-home" value="123,456"/>"""
                      )
                    }
                  case _ =>
                    ZIO.effectTotal {
                      Response(
                        status = 404,
                        headers = Map.empty,
                        body = ""
                      )
                    }
          }
        for
          sent <- ZQueue.unbounded[Tx]
          inbox <- MessageQueues
            .loop(new MessageLogger(sent))
            .inject(TestDb(), http)
          _ <- inbox.offer(
            Rx("#foo", "bar", "@zillow 123 main st., cityville, st")
          )
          _ <- TestClock.adjust(1.seconds)
          m <- sent.take
        yield assert(m)(equalTo(Tx("#foo", "Zestimate: $123,456")))
      } @@ timeout(2.seconds)
    )
