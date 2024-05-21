package sectery.usecases.responders

import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class VersionResponderSuite extends FunSuite:

  test("@version produces version") {

    val obtained: List[Tx] =
      new VersionResponder[Id]
        .respondToMessage(Rx("#foo", "bar", "@version"))

    val expected: List[Tx] =
      List(Tx("#foo", "0.1.0-SNAPSHOT"))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }
