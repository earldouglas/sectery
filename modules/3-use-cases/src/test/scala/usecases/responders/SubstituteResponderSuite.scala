package sectery.usecases.responders

import java.time.Instant
import munit.FunSuite
import sectery.domain.entities._
import sectery.effects.LastMessage.LastRx
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class SubstituteResponderSuite extends FunSuite:

  test("Last message is saved") {

    var lastMessage: Option[Rx] = None

    given substitute: LastMessage[Id] with
      override def saveLastMessage(rx: Rx): Id[Unit] =
        lastMessage = Some(rx)
      override def getLastMessages(
          service: String,
          channel: String
      ): Id[List[LastRx]] =
        ???

    val obtained: List[Tx] =
      new SubstituteResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "bar bar baz")
        )

    val expected: List[Tx] =
      Nil

    assertEquals(
      obtained = obtained,
      expected = expected
    )

    assertEquals(
      obtained = lastMessage,
      expected = Some(
        Rx("irc", "#foo", None, "bar", "bar bar baz")
      )
    )
  }

  test("s/bar/baz/ replaces the most recent bar with baz") {

    given substitute: LastMessage[Id] with
      override def saveLastMessage(rx: Rx): Id[Unit] =
        ()
      override def getLastMessages(
          service: String,
          channel: String
      ): Id[List[LastRx]] =
        channel match
          case "#foo" =>
            List(
              LastRx(
                "irc",
                "#foo",
                "alice",
                "bar",
                Instant.EPOCH.minusSeconds(2)
              ),
              LastRx(
                "irc",
                "#foo",
                "bob",
                "bar",
                Instant.EPOCH.minusSeconds(1)
              ),
              LastRx(
                "irc",
                "#foo",
                "charlie",
                "bar",
                Instant.EPOCH
              )
            )

    val obtained: List[Tx] =
      new SubstituteResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "s/bar/baz/")
        )

    val expected: List[Tx] =
      List(Tx("irc", "#foo", None, "<charlie> baz"))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("s/bar/baz/ replaces every bar with baz") {

    given substitute: LastMessage[Id] with
      override def saveLastMessage(rx: Rx): Id[Unit] =
        ()
      override def getLastMessages(
          service: String,
          channel: String
      ): Id[List[LastRx]] =
        channel match
          case "#foo" =>
            List(
              LastRx(
                "irc",
                "#foo",
                "jdoe",
                "bar bar baz",
                Instant.EPOCH
              )
            )

    val obtained: List[Tx] =
      new SubstituteResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "s/bar/baz/g")
        )

    val expected: List[Tx] =
      List(Tx("irc", "#foo", None, "<jdoe> baz baz baz"))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }
