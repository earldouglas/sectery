package sectery.usecases.responders

import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given
import java.time.Instant

class TellResponderSuite extends FunSuite:

  test("@tell saves a message for later") {

    var saved: List[String] = Nil

    given now: Now[Id] with
      override def now() = Instant.EPOCH

    given tell: Tell[Id] with
      override def save(channel: String, m: Tell.Saved) =
        (channel, m.to) match
          case ("#foo", "bob") =>
            saved = saved :+ m.message
      override def pop(channel: String, nick: String) = ???

    val obtained: List[Tx] =
      new TellResponder[Id]
        .respondToMessage(
          Rx("#foo", "bar", "@tell bob remember the milk")
        )

    val expected: List[Tx] =
      List(Tx("#foo", "I will let bob know."))

    assertEquals(
      obtained = obtained,
      expected = expected
    )

    assertEquals(
      obtained = saved,
      expected = List("remember the milk")
    )
  }

  test("@tell relays a saved message") {

    var saved: List[String] =
      List("remember the milk")

    given now: Now[Id] with
      override def now() =
        Instant.now()

    given tell: Tell[Id] with
      override def save(channel: String, m: Tell.Saved) = ???
      override def pop(channel: String, nick: String) =
        (channel, nick) match
          case ("#foo", "bob") =>
            val popped: List[String] = saved
            saved = Nil
            popped.map { message =>
              Tell.Saved(
                to = "bob",
                from = "bar",
                date = now.now().minusSeconds(60 * 60),
                message = message
              )
            }

    val obtained: List[Tx] =
      new TellResponder[Id]
        .respondToMessage(
          Rx("#foo", "bob", "What'd I miss?")
        )

    val expected: List[Tx] =
      List(Tx("#foo", "bob: 1 hour ago, bar said: remember the milk"))

    assertEquals(
      obtained = obtained,
      expected = expected
    )

    assertEquals(
      obtained = saved,
      expected = Nil
    )
  }
