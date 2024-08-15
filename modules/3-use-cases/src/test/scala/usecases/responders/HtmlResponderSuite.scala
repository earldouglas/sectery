package sectery.usecases.responders

import java.net.URL
import munit.FunSuite
import sectery.domain.entities._
import sectery.effects.HttpClient
import sectery.effects.HttpClient.Response
import sectery.effects.id.Id
import sectery.effects.id.given

class HtmlResponderSuite extends FunSuite:

  test("URL produces description from HTML") {

    given httpClient: HttpClient[Id] with
      override def request(
          method: String,
          url: URL,
          headers: Map[String, String],
          body: Option[String]
      ): Id[Response] =
        (method, url.toString()) match
          case ("GET", "https://example.com/hello.html") =>
            Response(
              status = 200,
              headers = Map.empty,
              body = """|<!DOCTYPE html>
                        |<html lang="en" dir="ltr">
                        |  <head>
                        |    <meta charset="utf-8">
                        |    <meta name="description" content="A page about hello, world.">
                        |    <title>Hello, world!</title>
                        |  </head>
                        |  <body>
                        |    <h1>Hello, world!</h1>
                        |  </body>
                        |</html>
                        |""".stripMargin
            )

    val obtained: List[Tx] =
      new HtmlResponder[Id]
        .respondToMessage(
          Rx(
            "irc",
            "#foo",
            None,
            "bar",
            "foo bar https://example.com/hello.html baz"
          )
        )

    val expected: List[Tx] =
      List(
        Tx(
          "irc",
          "#foo",
          None,
          "Hello, world!"
        ),
        Tx(
          "irc",
          "#foo",
          None,
          "A page about hello, world."
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }
