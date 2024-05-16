package sectery.adaptors

import java.net.URL
import sectery._
import sectery.effects._
import sectery.effects.id._
import sectery.effects.id.given
import munit.FunSuite
import sectery.effects.HttpClient.Response

class LiveStockSuite extends FunSuite:

  given httpClient: HttpClient[Id] =
    new HttpClient:
      override def request(
          method: String,
          url: URL,
          headers: Map[String, String],
          body: Option[String]
      ): Id[Response] =
        url.toString() match
          case "https://finnhub.io/api/v1/quote?symbol=VOO&token=alligator3" =>
            Response(
              status = 200,
              headers = Map.empty,
              body = Resource.read(
                "/io/finnhub/api/v1/quote?symbol=VOO&token=alligator3"
              )
            )
          case "https://finnhub.io/api/v1/quote?symbol=FOO&token=alligator3" =>
            Response(
              status = 200,
              headers = Map.empty,
              body = Resource.read(
                "/io/finnhub/api/v1/quote?symbol=FOO&token=alligator3"
              )
            )

  test("Retrieve quote for VOO") {
    assertEquals(
      obtained = LiveStock("alligator3").getQuote("VOO"),
      expected = Some("VOO: 390.09 +0.68 (+0.17%)")
    )
  }

  test("Fail to find quote for FOO") {
    assertEquals(
      obtained = LiveStock("alligator3").getQuote("FOO"),
      expected = None
    )
  }
