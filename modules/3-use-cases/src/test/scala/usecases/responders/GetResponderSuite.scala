package sectery.usecases.responders

import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class GetResponderSuite extends FunSuite:

  test("@get <key> when unset") {

    given getConfig: GetConfig[Id] with
      override def getConfig(
          nick: String,
          key: String
      ): Id[Option[String]] =
        None

    val obtained: List[Tx] =
      new GetResponder[Id]
        .respondToMessage(Rx("#foo", "bar", "@get foo"))

    val expected: List[Tx] =
      List(Tx("#foo", "bar: foo is not set"))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("@get <key> when set") {

    given getConfig: GetConfig[Id] with
      override def getConfig(
          nick: String,
          key: String
      ): Id[Option[String]] =
        (nick, key) match {
          case ("bar", "foo") =>
            Some("bar baz")
        }

    val obtained: List[Tx] =
      new GetResponder[Id]
        .respondToMessage(Rx("#foo", "bar", "@get foo"))

    val expected: List[Tx] =
      List(Tx("#foo", "bar: foo is set to bar baz"))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }
