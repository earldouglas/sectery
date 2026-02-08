package sectery.adaptors

import java.net.URL
import munit.FunSuite
import sectery._
import sectery.effects.HttpClient.Response
import sectery.effects._
import sectery.effects.id._
import sectery.effects.id.given

class LiveMorbotronSuite extends FunSuite:

  given http: HttpClient[Id] =
    new HttpClient:
      override def request(
          method: String,
          url: URL,
          headers: Map[String, String],
          body: Option[String]
      ): Id[Response] =
        url.toString() match
          case "https://morbotron.com/api/search?q=windmills+do+not+work+that+way" =>
            Response(
              status = 200,
              headers = Map.empty,
              body = Resource.read(
                "/com/morbotron/api/search?q=windmills+do+not+work+that+way"
              )
            )
          case "https://morbotron.com/api/caption?e=S05E01&t=378945" =>
            Response(
              status = 200,
              headers = Map.empty,
              body = Resource.read(
                "/com/morbotron/api/caption?e=S05E01&t=378945"
              )
            )

  val morbotron: Morbotron[Id] = LiveMorbotron()

  test("Retrieve caption from morbotron.com") {

    val expected: List[String] =
      List(
        "https://morbotron.com/caption/S05E01/378945",
        "I'm sure those windmills will keep them cool.",
        "Windmills do not work that way!",
        "Good night!"
      )

    val obtained: List[String] =
      morbotron.morbotron("windmills do not work that way")

    assertEquals(
      obtained = obtained.toList,
      expected = expected.toList
    )
  }
