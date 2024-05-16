package sectery.usecases.responders

import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given
import java.time.Instant

class GrabResponderSuite extends FunSuite:

  test("@grab [nick] can't grab own message") {

    given now: Now[Id] with
      override def now() = Instant.EPOCH

    given lastMessage: LastMessage[Id] with
      override def getLastMessages(channel: String) = ???
      override def saveLastMessage(rx: Rx) = ???

    given grab: Grab[Id] with
      override def grab(channel: String, nick: String) = false
      override def grab(channel: String) = ???

    val obtained: List[Tx] =
      new GrabResponder[Id]
        .respondToMessage(Rx("#foo", "bar", "@grab bar"))

    val expected: List[Tx] =
      List(Tx("#foo", "You can't grab yourself."))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("@grab [nick] can't grab absent message") {

    given now: Now[Id] with
      override def now() = Instant.EPOCH

    given lastMessage: LastMessage[Id] with
      override def getLastMessages(channel: String) = ???
      override def saveLastMessage(rx: Rx) = ???

    given grab: Grab[Id] with
      override def grab(channel: String, nick: String) = false
      override def grab(channel: String) = ???

    val obtained: List[Tx] =
      new GrabResponder[Id]
        .respondToMessage(Rx("#foo", "bar", "@grab baz"))

    val expected: List[Tx] =
      List(Tx("#foo", "baz hasn't said anything."))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("@grab [nick] grabs nick's message") {

    given now: Now[Id] with
      override def now() = Instant.EPOCH

    given lastMessage: LastMessage[Id] with
      override def getLastMessages(channel: String) = ???
      override def saveLastMessage(rx: Rx) = ???

    given grab: Grab[Id] with
      override def grab(channel: String, nick: String) =
        (channel, nick) match {
          case ("#foo", "baz") => true
        }
      override def grab(channel: String) = ???

    val obtained: List[Tx] =
      new GrabResponder[Id]
        .respondToMessage(Rx("#foo", "bar", "@grab baz"))

    val expected: List[Tx] =
      List(Tx("#foo", "Grabbed baz."))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("@grab can't grab absent message last message") {

    given now: Now[Id] with
      override def now() = Instant.EPOCH

    given lastMessage: LastMessage[Id] with
      override def getLastMessages(channel: String) = ???
      override def saveLastMessage(rx: Rx) = ???

    given grab: Grab[Id] with
      override def grab(channel: String, nick: String) = ???
      override def grab(channel: String) = None

    val obtained: List[Tx] =
      new GrabResponder[Id]
        .respondToMessage(Rx("#foo", "bar", "@grab"))

    val expected: List[Tx] =
      List(Tx("#foo", "Nobody has said anything."))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("@grab grabs last message") {

    given now: Now[Id] with
      override def now() = Instant.EPOCH

    given lastMessage: LastMessage[Id] with
      override def getLastMessages(channel: String) = ???
      override def saveLastMessage(rx: Rx) = ???

    given grab: Grab[Id] with
      override def grab(channel: String, nick: String) = ???
      override def grab(channel: String) =
        channel match {
          case "#foo" => Some("baz")
        }

    val obtained: List[Tx] =
      new GrabResponder[Id]
        .respondToMessage(Rx("#foo", "bar", "@grab"))

    val expected: List[Tx] =
      List(Tx("#foo", "Grabbed baz."))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("Save non-@grab message") {

    var saved: Option[Rx] = None

    given now: Now[Id] with
      override def now() = Instant.EPOCH

    given lastMessage: LastMessage[Id] with
      override def getLastMessages(channel: String) = ???
      override def saveLastMessage(rx: Rx) =
        saved = Some(rx)

    given grab: Grab[Id] with
      override def grab(channel: String, nick: String) = ???
      override def grab(channel: String) = ???

    val obtained: List[Tx] =
      new GrabResponder[Id]
        .respondToMessage(Rx("#foo", "bar", "Heyo."))

    val expected: List[Tx] =
      Nil

    assertEquals(
      obtained = obtained,
      expected = expected
    )

    assertEquals(
      obtained = saved,
      expected = Some(Rx("#foo", "bar", "Heyo."))
    )
  }
