package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.duration._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test._
import zio.test.environment.TestClock

object HtmlSpec extends DefaultRunnableSpec:
  override def spec =
    suite(getClass().getName())(
      testM("URL produces description from HTML") {
        for
          sent <- ZQueue.unbounded[Tx]
          http = sys.env.get("TEST_HTTP_LIVE") match
            case Some("true") => Http.live
            case _ =>
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
                        case "https://earldouglas.com/posts/scala.html" =>
                          ZIO.effectTotal {
                            Response(
                              status = 200,
                              headers = Map.empty,
                              body = """|<html>
                                                  |  <head>
                                                  |    <meta   name="description"   content="Some notes on the Scala language, 
                                                  |libraries, and ecosystem."  >
                                                  |  </head>
                                                  |</html>
                                                  |""".stripMargin
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
              http
          inbox <- MessageQueues
            .loop(new MessageLogger(sent))
            .inject(TestDb(), http)
          _ <- inbox.offer(
            Rx(
              "#foo",
              "bar",
              "foo bar https://earldouglas.com/posts/scala.html baz"
            )
          )
          _ <- TestClock.adjust(1.seconds)
          m <- sent.take
        yield assert(m)(
          equalTo(
            Tx(
              "#foo",
              "Some notes on the Scala language, libraries, and ecosystem."
            )
          )
        )
      } @@ timeout(2.seconds)
    )
