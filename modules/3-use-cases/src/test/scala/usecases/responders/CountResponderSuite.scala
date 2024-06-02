package sectery.usecases.responders

import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class CountResponderSuite extends FunSuite:

  test("@count produces count") {

    given counter: Counter[Id] with
      override def incrementAndGet(): Id[Int] = 42

    val obtained: List[Tx] =
      new CountResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@count")
        )

    val expected: List[Tx] =
      List(Tx("irc", "#foo", None, "42"))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }
