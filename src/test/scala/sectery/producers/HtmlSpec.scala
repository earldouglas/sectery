package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test.TestClock
import zio.test._

object HtmlSpec extends DefaultRunnableSpec:
  override def spec =
    suite(getClass().getName())(
      test("URL produces description from HTML") {
        for
          _ <- ZIO.succeed(())
          http = sys.env.get("TEST_HTTP_LIVE") match
            case Some("true") => Http.live
            case _ =>
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
                        case "https://earldouglas.com/posts/scala.html" =>
                          ZIO.succeed {
                            Response(
                              status = 200,
                              headers = Map.empty,
                              body = Resource.read(
                                "/com/earldouglas/posts/scala.html"
                              )
                            )
                          }
                        case _ =>
                          ZIO.succeed {
                            Response(
                              status = 404,
                              headers = Map.empty,
                              body = ""
                            )
                          }
                }
              http
          (inbox, outbox, _) <- MessageQueues.loop
            .inject_(TestDb(), http)
          _ <- inbox.offer(
            Rx(
              "#foo",
              "bar",
              "foo bar https://earldouglas.com/posts/scala.html baz"
            )
          )
          _ <- TestClock.adjust(1.seconds)
          ms <- outbox.takeAll
        yield assert(ms)(
          equalTo(
            List(
              Tx(
                "#foo",
                "Notes on Scala: Some notes on the Scala language, libraries, and ecosystem."
              )
            )
          )
        )
      } @@ timeout(2.seconds)
    )
