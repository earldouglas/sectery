package sectery.usecases.responders

import java.time.Instant
import munit.FunSuite
import sectery.domain.entities._
import sectery.effects.LastMessage.LastRx
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class GrabResponderSuite extends FunSuite:

  test("@grab [nick] can't grab own message") {

    given lastMessage: LastMessage[Id] with
      override def getLastMessages(service: String, channel: String) =
        ???
      override def saveLastMessage(rx: Rx) = ???

    given grab: Grab[Id] with
      override def grab(
          service: String,
          channel: String,
          nick: String
      ) = false

    val obtained: List[Tx] =
      new GrabResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@grab bar")
        )

    val expected: List[Tx] =
      List(
        Tx(
          "irc",
          "#foo",
          None,
          "You can't grab yourself."
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("@grab [nick] can't grab absent message") {

    given lastMessage: LastMessage[Id] with
      override def getLastMessages(service: String, channel: String) =
        (service, channel) match
          case ("irc", "#foo") => Nil
          case _               => throw new Exception("unexpected")
      override def saveLastMessage(rx: Rx) = ???

    given grab: Grab[Id] with
      override def grab(
          service: String,
          channel: String,
          nick: String
      ) = false

    val obtained: List[Tx] =
      new GrabResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@grab baz")
        )

    val expected: List[Tx] =
      List(
        Tx(
          "irc",
          "#foo",
          None,
          "baz hasn't said anything."
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("@grab [nick] grabs nick's message") {

    given lastMessage: LastMessage[Id] with
      override def getLastMessages(service: String, channel: String) =
        ???
      override def saveLastMessage(rx: Rx) = ???

    given grab: Grab[Id] with
      override def grab(
          service: String,
          channel: String,
          nick: String
      ) =
        (channel, nick) match {
          case ("#foo", "baz") => true
          case _               => throw new Exception("unexpected")
        }

    val obtained: List[Tx] =
      new GrabResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@grab baz")
        )

    val expected: List[Tx] =
      List(Tx("irc", "#foo", None, "Grabbed baz."))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("@grab can't grab absent message last message") {

    given lastMessage: LastMessage[Id] with
      override def getLastMessages(service: String, channel: String) =
        (service, channel) match {
          case ("irc", "#foo") => Nil
          case _               => throw new Exception("unexpected")
        }
      override def saveLastMessage(rx: Rx) = ???

    given grab: Grab[Id] with
      override def grab(
          service: String,
          channel: String,
          nick: String
      ) = ???

    val obtained: List[Tx] =
      new GrabResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@grab")
        )

    val expected: List[Tx] =
      List(
        Tx(
          "irc",
          "#foo",
          None,
          "Nobody has said anything."
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("@grab grabs last message") {

    given lastMessage: LastMessage[Id] with
      override def getLastMessages(service: String, channel: String) =
        (service, channel) match
          case ("irc", "#foo") =>
            List(
              LastRx("irc", "#foo", "baz", "I am baz.", Instant.EPOCH)
            )
          case _ => throw new Exception("unexpected")
      override def saveLastMessage(rx: Rx) = ???

    given grab: Grab[Id] with
      override def grab(
          service: String,
          channel: String,
          nick: String
      ) =
        (service, channel, nick) match
          case ("irc", "#foo", "baz") => true
          case _ => throw new Exception("unexpected")

    val obtained: List[Tx] =
      new GrabResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@grab")
        )

    val expected: List[Tx] =
      List(Tx("irc", "#foo", None, "Grabbed baz."))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("Save non-@grab message") {

    var saved: Option[Rx] = None

    given lastMessage: LastMessage[Id] with
      override def getLastMessages(service: String, channel: String) =
        ???
      override def saveLastMessage(rx: Rx) =
        saved = Some(rx)

    given grab: Grab[Id] with
      override def grab(
          service: String,
          channel: String,
          nick: String
      ) = ???

    val obtained: List[Tx] =
      new GrabResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "Heyo.")
        )

    val expected: List[Tx] =
      Nil

    assertEquals(
      obtained = obtained,
      expected = expected
    )

    assertEquals(
      obtained = saved,
      expected = Some(Rx("irc", "#foo", None, "bar", "Heyo."))
    )
  }
