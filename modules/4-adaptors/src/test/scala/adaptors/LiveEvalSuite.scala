package sectery.adaptors

import java.net.URL
import munit.FunSuite
import sectery._
import sectery.effects.HttpClient.Response
import sectery.effects._
import sectery.effects.id._
import sectery.effects.id.given

class LiveEvalSuite extends FunSuite:

  given http: HttpClient[Id] =
    new HttpClient:
      override def request(
          method: String,
          url: URL,
          headers: Map[String, String],
          body: Option[String]
      ): Id[Response] =
        url.toString() match
          case "https://earldouglas.com/api/haskeval?src=6+*+7" =>
            Response(
              status = 200,
              headers = Map.empty,
              body = "42"
            )

  val eval: Eval[Id] = LiveEval()

  test("@eval 6 * 7") {
    assertEquals(
      obtained = eval.eval("6 * 7"),
      expected = Right("42")
    )
  }
