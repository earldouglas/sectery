package sectery.adaptors

import java.net.URL
import munit.FunSuite
import sectery._
import sectery.effects.HttpClient.Response
import sectery.effects._
import sectery.effects.id._
import sectery.effects.id.given

class LiveFrinkiacSuite extends FunSuite:

  given http: HttpClient[Id] =
    new HttpClient:
      override def request(
          method: String,
          url: URL,
          headers: Map[String, String],
          body: Option[String]
      ): Id[Response] =
        url.toString() match
          case "https://frinkiac.com/api/search?q=i+got+to+think+of+a+lie+fast" =>
            Response(
              status = 200,
              headers = Map.empty,
              body = Resource.read(
                "/com/frinkiac/api/search?q=i+got+to+think+of+a+lie+fast"
              )
            )
          case "https://frinkiac.com/api/caption?e=S04E16&t=166465" =>
            Response(
              status = 200,
              headers = Map.empty,
              body = Resource.read(
                "/com/frinkiac/api/caption?e=S04E16&t=166465"
              )
            )

  val frinkiac: Frinkiac[Id] = LiveFrinkiac()

  test("Retrieve caption rate from frinkiac.com") {

    val expected: List[String] =
      List(
        "https://frinkiac.com/caption/S04E16/166465",
        "DID I SAY THAT OR JUST THINK IT?",
        "I GOT TO THINK OF A LIE FAST.",
        "HOMER, ARE YOU GOING TO THE DUFF BREWERY?"
      )

    val obtained: List[String] =
      frinkiac.frinkiac("i got to think of a lie fast")

    assertEquals(
      obtained = obtained.toList,
      expected = expected.toList
    )
  }
