package sectery.usecases.responders

import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given
import java.time.Instant

class TimeResponderSuite extends FunSuite:

  test("@time produces help with no user-configured tz") {

    given getConfig: GetConfig[Id] with
      override def getConfig(
          nick: String,
          key: String
      ): Id[Option[String]] =
        None

    given now: Now[Id] with
      override def now(): Id[Instant] = ???

    val obtained: List[Tx] =
      new TimeResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@time")
        )

    val expected: List[Tx] =
      List(
        Tx(
          "irc",
          "#foo",
          None,
          "bar: Set default time zone with `@set tz <zone>`"
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("@time <zone> produces time, ignoring user-configured tz") {

    given getConfig: GetConfig[Id] with
      override def getConfig(
          nick: String,
          key: String
      ): Id[Option[String]] =
        (nick, key) match {
          case ("bar", "tz") =>
            Some("PST")
        }

    given now: Now[Id] with
      override def now(): Id[Instant] =
        Instant.ofEpochMilli(1234567890L)

    val obtained: List[Tx] =
      new TimeResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@time MST")
        )

    val expected: List[Tx] =
      List(
        Tx(
          "irc",
          "#foo",
          None,
          "Wed, 14 Jan 1970, 23:56 MST"
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("@time <zone> produces time with no user-configured tz") {

    given getConfig: GetConfig[Id] with
      override def getConfig(
          nick: String,
          key: String
      ): Id[Option[String]] =
        None

    given now: Now[Id] with
      override def now(): Id[Instant] =
        Instant.ofEpochMilli(1234567890L)

    val obtained: List[Tx] =
      new TimeResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@time MST")
        )

    val expected: List[Tx] =
      List(
        Tx(
          "irc",
          "#foo",
          None,
          "Wed, 14 Jan 1970, 23:56 MST"
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("@time produces time in user-configured tz") {

    given getConfig: GetConfig[Id] with
      override def getConfig(
          nick: String,
          key: String
      ): Id[Option[String]] =
        Some("PST")

    given now: Now[Id] with
      override def now(): Id[Instant] =
        Instant.ofEpochMilli(1234567890L)

    val obtained: List[Tx] =
      new TimeResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@time")
        )

    val expected: List[Tx] =
      List(
        Tx(
          "irc",
          "#foo",
          None,
          "Wed, 14 Jan 1970, 22:56 PST"
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }
