package sectery.usecases.responders

import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class PointsResponderSuite extends FunSuite:

  test("<nick>++ increases points to 1") {

    given points: Points[Id] with
      override def update(
          service: String,
          channel: String,
          nick: String,
          delta: Int
      ) =
        (service, channel, nick, delta) match
          case ("irc", "#foo", "jdoe", 1) => 1

    val obtained: List[Tx] =
      new PointsResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "jdoe++")
        )

    val expected: List[Tx] =
      List(Tx("irc", "#foo", None, "jdoe has 1 point."))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("<nick>++ increases points to 2") {

    given points: Points[Id] with
      override def update(
          service: String,
          channel: String,
          nick: String,
          delta: Int
      ) =
        (service, channel, nick, delta) match
          case ("irc", "#foo", "jdoe", 1) => 2

    val obtained: List[Tx] =
      new PointsResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "jdoe++")
        )

    val expected: List[Tx] =
      List(Tx("irc", "#foo", None, "jdoe has 2 points."))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("<nick>-- decreases points to 1") {

    given points: Points[Id] with
      override def update(
          service: String,
          channel: String,
          nick: String,
          delta: Int
      ) =
        (service, channel, nick, delta) match
          case ("irc", "#foo", "jdoe", -1) => 1

    val obtained: List[Tx] =
      new PointsResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "jdoe--")
        )

    val expected: List[Tx] =
      List(Tx("irc", "#foo", None, "jdoe has 1 point."))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("<nick>-- decreases points to 0") {

    given points: Points[Id] with
      override def update(
          service: String,
          channel: String,
          nick: String,
          delta: Int
      ) =
        (service, channel, nick, delta) match
          case ("irc", "#foo", "jdoe", -1) => 0

    val obtained: List[Tx] =
      new PointsResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "jdoe--")
        )

    val expected: List[Tx] =
      List(Tx("irc", "#foo", None, "jdoe has 0 points."))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("Can't increase own points") {

    given points: Points[Id] with
      override def update(
          service: String,
          channel: String,
          nick: String,
          delta: Int
      ) = ???

    val obtained: List[Tx] =
      new PointsResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "bar++")
        )

    val expected: List[Tx] =
      List(
        Tx(
          "irc",
          "#foo",
          None,
          "You gotta get someone else to do it."
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }
  test("Can't decrease own points") {

    given points: Points[Id] with
      override def update(
          service: String,
          channel: String,
          nick: String,
          delta: Int
      ) = ???

    val obtained: List[Tx] =
      new PointsResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "bar--")
        )

    val expected: List[Tx] =
      List(
        Tx(
          "irc",
          "#foo",
          None,
          "You gotta get someone else to do it."
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }
