package sectery.usecases.responders

import java.time.Instant
import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class QuoteResponderSuite extends FunSuite:

  test("@quote can't quote ungrabbed channel") {

    given quote: Quote[Id] with
      override def quote(
          service: String,
          channel: String,
          nick: String
      ) = None
      override def quote(service: String, channel: String) = None

    val obtained: List[Tx] =
      new QuoteResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@quote")
        )

    val expected: List[Tx] =
      List(
        Tx(
          "irc",
          "#foo",
          None,
          "Nobody has been grabbed."
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("@quote can't quote ungrabbed nick") {

    given quote: Quote[Id] with
      override def quote(
          service: String,
          channel: String,
          nick: String
      ) = None
      override def quote(service: String, channel: String) = None

    val obtained: List[Tx] =
      new QuoteResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@quote baz")
        )

    val expected: List[Tx] =
      List(
        Tx(
          "irc",
          "#foo",
          None,
          "baz hasn't been grabbed."
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("@quote quotes logged message") {

    given quote: Quote[Id] with
      override def quote(
          service: String,
          channel: String,
          nick: String
      ) =
        (channel, nick) match {
          case ("#foo", "baz") =>
            Some(
              Quote.GrabbedMessage(
                service = service,
                channel = channel,
                nick = nick,
                message = "I can't believe I ate the whole thing.",
                instant = Instant.EPOCH
              )
            )
        }
      override def quote(service: String, channel: String) = None

    val obtained: List[Tx] =
      new QuoteResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@quote baz")
        )

    val expected: List[Tx] =
      List(
        Tx(
          "irc",
          "#foo",
          None,
          "<baz> I can't believe I ate the whole thing."
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }
