package sectery.usecases.responders

import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class EvalResponderSuite extends FunSuite:

  test("@eval <expression> produces result") {

    given haskeval: Eval[Id] with
      override def eval(src: String): Id[Either[String, String]] =
        src match {
          case "6 * 7" =>
            Right("42")
        }

    val obtained: List[Tx] =
      new EvalResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@eval 6 * 7")
        )

    val expected: List[Tx] =
      List(Tx("irc", "#foo", None, "42"))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }
