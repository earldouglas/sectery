package sectery.producers

import sectery._
import sectery._
import zio._

object HtmlSpec extends ProducerSpec:

  override val http =
    sys.env.get("TEST_HTTP_LIVE") match
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
              ): ZIO[Any, Nothing, Response] =
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

  override val specs =
    Map(
      "URL produces description from HTML" ->
        (
          List(
            Rx(
              "#foo",
              "bar",
              "foo bar https://earldouglas.com/posts/scala.html baz"
            )
          ),
          List(
            Tx(
              "#foo",
              "Notes on Scala: Some notes on the Scala language, libraries, and ecosystem."
            )
          )
        )
    )
