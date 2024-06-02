package sectery.usecases.responders

import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class FrinkiacResponderSuite extends FunSuite:

  test("@frinkiac <quote> produces caption") {

    given frinkiac: Frinkiac[Id] with
      override def frinkiac(q: String): Id[List[String]] =
        q match {
          case "foo" => List("foo bar baz")
        }

    val obtained: List[Tx] =
      new FrinkiacResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@frinkiac foo")
        )

    val expected: List[Tx] =
      List(Tx("irc", "#foo", None, "foo bar baz"))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }
