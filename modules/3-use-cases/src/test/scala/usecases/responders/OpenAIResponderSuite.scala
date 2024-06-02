package sectery.usecases.responders

import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class OpenAIResponderSuite extends FunSuite:

  test("@ai <prompt> produces completion") {

    given openAI: OpenAI[Id] with
      override def complete(prompt: String) =
        prompt match
          case "What is the answer to life, the universe, and everything?" =>
            List("42")

    val obtained: List[Tx] =
      new OpenAIResponder[Id]
        .respondToMessage(
          Rx(
            "irc",
            "#foo",
            None,
            "bar",
            "@ai What is the answer to life, the universe, and everything?"
          )
        )

    val expected: List[Tx] =
      List(Tx("irc", "#foo", None, "42"))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }
