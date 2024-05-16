package sectery.usecases.responders

import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class BtcResponderSuite extends FunSuite:

  test("@btc produces rate") {

    given btc: Btc[Id] with
      override def toUsd(): Id[Option[Float]] =
        Some(39193.03f)

    val obtained: List[Tx] =
      new BtcResponder[Id]
        .respondToMessage(Rx("#foo", "bar", "@btc"))

    val expected: List[Tx] =
      List(Tx("#foo", "$39,193.03"))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }
