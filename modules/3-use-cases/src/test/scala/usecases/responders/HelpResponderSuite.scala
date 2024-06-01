package sectery.usecases.responders

import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class HelpResponderSuite extends FunSuite:

  test("@help produces list of responders") {

    val helpResponder: HelpResponder[Id] =
      new HelpResponder(
        List(
          new PingResponder,
          new AsciiResponder
        )
      )

    val obtained: List[Tx] =
      helpResponder
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@help")
        )

    val expected: List[Tx] =
      List(
        Tx("irc", "#foo", None, "@ascii, @help, @ping")
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("@help <responder> produces respnoder usage") {

    val helpResponder: HelpResponder[Id] =
      new HelpResponder(
        List(
          new PingResponder,
          new AsciiResponder
        )
      )

    val obtained: List[Tx] =
      helpResponder
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@help @ascii")
        )

    val expected: List[Tx] =
      List(
        Tx("irc", "#foo", None, "Usage: @ascii <text>")
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }
