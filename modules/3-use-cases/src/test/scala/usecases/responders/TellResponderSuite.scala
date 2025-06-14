package sectery.usecases.responders

import java.time.Instant
import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class TellResponderSuite extends FunSuite:

  test("@tell saves a message for later") {

    var saved: List[String] = Nil

    given now: Now[Id] with
      override def now() = Instant.EPOCH

    given tell: Tell[Id] with
      override def save(
          service: String,
          channel: String,
          m: Tell.Saved
      ) =
        (service, channel, m.to) match
          case ("irc", "#foo", "bob") =>
            saved = saved :+ m.message
          case _ => throw new Exception("unexpected")

      override def pop(
          service: String,
          channel: String,
          nick: String
      ) = ???

    val obtained: List[Tx] =
      new TellResponder[Id]
        .respondToMessage(
          Rx(
            "irc",
            "#foo",
            None,
            "bar",
            "@tell bob remember the milk"
          )
        )

    val expected: List[Tx] =
      List(
        Tx("irc", "#foo", None, "I will let bob know.")
      )

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

      override def save(
          service: String,
          channel: String,
          m: Tell.Saved
      ) = ???

      override def pop(
          service: String,
          channel: String,
          nick: String
      ) =
        (service, channel, nick) match
          case ("irc", "#foo", "bob") =>
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
          case _ => throw new Exception("unexpected")

    val obtained: List[Tx] =
      new TellResponder[Id]
        .respondToMessage(
          Rx(
            "irc",
            "#foo",
            None,
            "bob",
            "What'd I miss?"
          )
        )

    val expected: List[Tx] =
      List(
        Tx(
          "irc",
          "#foo",
          None,
          "bob: 1 hour ago, bar said: remember the milk"
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )

    assertEquals(
      obtained = saved,
      expected = Nil
    )
  }
