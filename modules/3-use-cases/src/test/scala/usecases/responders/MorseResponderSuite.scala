package sectery.usecases.responders

import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class MorseResponderSuite extends FunSuite:

  val encoded: String =
    List(
      "- .... .",
      "--.- ..- .. -.-. -.-",
      "-... .-. --- .-- -.",
      "..-. --- -..-",
      ".--- ..- -- .--. ...",
      "--- ...- . .-.",
      "- .... .",
      ".-.. .- --.. -.--",
      "-.. --- --.",
      ".---- ..--- ...-- ....- ..... -.... --... ---.. ----. -----"
    ).mkString(" / ")

  val decoded: String =
    "The quick brown fox jumps over the lazy dog 1234567890"

  test("@morse encode produces morse") {

    val obtained: List[Tx] =
      new MorseResponder[Id]
        .respondToMessage(
          Rx("#foo", "bar", s"@morse encode ${decoded}")
        )

    val expected: List[Tx] =
      List(Tx("#foo", encoded))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("@morse decode produces text") {

    val obtained: List[Tx] =
      new MorseResponder[Id]
        .respondToMessage(
          Rx("#foo", "bar", s"@morse decode ${encoded}")
        )

    val expected: List[Tx] =
      List(Tx("#foo", decoded.toUpperCase()))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }
