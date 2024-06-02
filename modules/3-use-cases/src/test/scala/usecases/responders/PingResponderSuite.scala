package sectery.usecases.responders

import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class PingResponderSuite extends FunSuite:

  test("@ping produces pong") {

    val obtained: List[Tx] =
      new PingResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@ping")
        )

    val expected: List[Tx] =
      List(Tx("irc", "#foo", None, "pong"))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }
