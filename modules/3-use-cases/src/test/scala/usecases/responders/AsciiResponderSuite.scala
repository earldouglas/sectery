package sectery.usecases.responders

import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class AsciiResponderSpec extends FunSuite:

  test("@ascii <text> produces ascii text") {

    // This can vary depending on the fonts installed on the system
    val asciiFoo: List[List[String]] =
      List(
        List(
          "  ##",
          " #",
          " #",
          "####  ####    ####",
          " #   ##  ##  ##  ##",
          " #   #    #  #    #",
          " #   #    #  #    #",
          " #   #    #  #    #",
          " #   ##  ##  ##  ##",
          " #    ####    ####"
        ),
        List(
          "    ##",
          "   #",
          "   #",
          " #####   ###    ###",
          "   #    #   #  #   #",
          "   #    #   #  #   #",
          "   #    #   #  #   #",
          "   #    #   #  #   #",
          "   #    #   #  #   #",
          "   #     ###    ###"
        )
      )

    val obtained: List[Tx] =
      new AsciiResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@ascii foo")
        )

    val expected: List[List[Tx]] =
      asciiFoo.map(
        _.map(line => Tx("irc", "#foo", None, line))
      )

    val found: Boolean =
      expected.find(_ == obtained).isDefined

    assert(found)
  }
