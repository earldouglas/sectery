package sectery.adaptors

import java.net.URL
import munit.FunSuite
import sectery._
import sectery.effects.HttpClient.Response
import sectery.effects._
import sectery.effects.id._
import sectery.effects.id.given

class LiveBtcSuite extends FunSuite:

  given http: HttpClient[Id] =
    new HttpClient:
      override def request(
          method: String,
          url: URL,
          headers: Map[String, String],
          body: Option[String]
      ): Id[Response] =
        url.toString() match
          case "https://api.coindesk.com/v1/bpi/currentprice.json" =>
            Response(
              status = 200,
              headers = Map.empty,
              body = Resource.read(
                "/com/coindesk/api/v1/bpi/currentprice.json"
              )
            )

  val btc: Btc[Id] = LiveBtc()

  test("Retrieve rate from Coindesk") {

    val expected: Option[Float] = Some(39193.027f)

    val obtained: Option[Float] = btc.toUsd()

    assertEquals(
      obtained = obtained.toList,
      expected = expected.toList
    )
  }
