package sectery.usecases.responders

import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class BlinkResponderSuite extends FunSuite:

  test("@blink <text> produces blinking text") {

    val obtained: List[Tx] =
      new BlinkResponder[Id]
        .respondToMessage(Rx("#foo", "bar", "@blink foo"))

    val expected: List[Tx] =
      List(Tx("#foo", "\u0006foo\u0006"))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }
