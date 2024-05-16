package sectery.adaptors

import java.net.URL
import sectery._
import sectery.effects._
import sectery.effects.id._
import sectery.effects.id.given
import munit.FunSuite
import sectery.effects.HttpClient.Response

class LiveOpenAISuite extends FunSuite:

  given http: HttpClient[Id] =
    new HttpClient:
      override def request(
          method: String,
          url: URL,
          headers: Map[String, String],
          body: Option[String]
      ): Id[Response] =
        url.toString() match
          case "https://api.openai.com/v1/chat/completions" =>
            Response(
              status = 200,
              headers = Map.empty,
              body = Resource.read(
                "/com/openai/api/v1/chat/completions"
              )
            )

  val openAi: OpenAI[Id] = LiveOpenAI("alligator3")

  test("Retrieve chat completion") {

    val expected: List[String] =
      List("Hi! How can I assist you today?")

    val obtained: List[String] =
      openAi.complete("Hello!")

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }
