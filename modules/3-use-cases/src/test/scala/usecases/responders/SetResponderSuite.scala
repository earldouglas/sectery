package sectery.usecases.responders

import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class SetResponderSuite extends FunSuite:

  test("@set foo to bar baz") {

    var state: Option[(String, String, String)] = None

    given setConfig: SetConfig[Id] with
      override def setConfig(
          nick: String,
          key: String,
          value: String
      ): Id[Unit] =
        (nick, key) match {
          case ("bar", "foo") =>
            state = Some((nick, key, value))
          case _ => throw new Exception("unexpected")
        }

    val obtained: List[Tx] =
      new SetResponder[Id]
        .respondToMessage(
          Rx(
            "irc",
            "#foo",
            None,
            "bar",
            "@set foo bar baz"
          )
        )

    val expected: List[Tx] =
      List(
        Tx(
          "irc",
          "#foo",
          None,
          "bar: foo set to bar baz"
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )

    assertEquals(
      obtained = state,
      expected = Some(("bar", "foo", "bar baz"))
    )
  }
