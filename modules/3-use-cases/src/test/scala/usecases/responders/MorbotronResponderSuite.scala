package sectery.usecases.responders

import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class MorbotronResponderSuite extends FunSuite:

  test("@morbotron <quote> produces caption") {

    given morbotron: Morbotron[Id] with
      override def morbotron(q: String): Id[List[String]] =
        q match {
          case "foo" => List("foo bar baz")
        }

    val obtained: List[Tx] =
      new MorbotronResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@morbotron foo")
        )

    val expected: List[Tx] =
      List(Tx("irc", "#foo", None, "foo bar baz"))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }
