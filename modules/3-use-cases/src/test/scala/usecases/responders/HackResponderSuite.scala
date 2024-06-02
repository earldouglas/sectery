package sectery.usecases.responders

import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class HackResponderSuite extends FunSuite:

  trait HackStub extends Hack[Id]:
    override def getOrStartGame(
        service: String,
        channel: String
    ): Id[(String, Int)] =
      ???
    override def isAWord(word: String): Id[Boolean] =
      ???
    override def deleteGame(
        service: String,
        channel: String
    ): Id[Unit] =
      ???
    override def setGuessCount(
        service: String,
        channel: String,
        guessCount: Int
    ): Id[Unit] =
      ???

  test("@hack: start a new game") {
    given hack: Hack[Id] =
      new HackStub:
        override def getOrStartGame(service: String, channel: String) =
          (service, channel) match
            case ("irc", "#foo") => ("foo", 0)

    assertEquals(
      new HackResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@hack")
        ),
      List(
        Tx(
          "irc",
          "#foo",
          None,
          "Guess a word with 3 letters.  0 tries so far."
        )
      )
    )
  }

  test("@hack: guess a non-word") {
    given hack: Hack[Id] =
      new HackStub:
        override def getOrStartGame(service: String, channel: String) =
          (service, channel) match
            case ("irc", "#foo") => ("foo", 1)
        override def isAWord(word: String) =
          word match
            case "bar" => false

    val obtained: List[Tx] =
      new HackResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@hack bar")
        )

    val expected: List[Tx] =
      List(
        Tx("irc", "#foo", None, "Guess an actual word.")
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("@hack: guess a valid word with no matching letters") {
    given hack: Hack[Id] =
      new HackStub:
        override def getOrStartGame(service: String, channel: String) =
          (service, channel) match
            case ("irc", "#foo") => ("foo", 1)
        override def isAWord(word: String) =
          word match
            case "bar" => true
        override def setGuessCount(
            service: String,
            channel: String,
            guessCount: Int
        ) =
          (channel, guessCount) match
            case ("#foo", 2) => ()

    val obtained: List[Tx] =
      new HackResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@hack bar")
        )

    val expected: List[Tx] =
      List(
        Tx(
          "irc",
          "#foo",
          None,
          "0/3 correct.  2 tries so far."
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("@hack: guess a valid word with some matching letters") {
    given hack: Hack[Id] =
      new HackStub:
        override def getOrStartGame(service: String, channel: String) =
          (service, channel) match
            case ("irc", "#foo") => ("foo", 2)
        override def isAWord(word: String) =
          word match
            case "foe" => true
        override def setGuessCount(
            service: String,
            channel: String,
            guessCount: Int
        ) =
          (channel, guessCount) match
            case ("#foo", 3) => ()

    val obtained: List[Tx] =
      new HackResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@hack foe")
        )

    val expected: List[Tx] =
      List(
        Tx(
          "irc",
          "#foo",
          None,
          "2/3 correct.  3 tries so far."
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("@hack: guess a the correct solution and delete the game") {
    var deleted: Boolean = false

    given hack: Hack[Id] =
      new HackStub:
        override def getOrStartGame(service: String, channel: String) =
          (service, channel) match
            case ("irc", "#foo") => ("foo", 3)
        override def isAWord(word: String) =
          word match
            case "foo" => true
        override def deleteGame(service: String, channel: String) =
          (service, channel) match
            case ("irc", "#foo") => deleted = true

    val obtained: List[Tx] =
      new HackResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@hack foo")
        )

    val expected: List[Tx] =
      List(
        Tx(
          "irc",
          "#foo",
          None,
          "Guessed foo in 4 tries."
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )

    assert(deleted)
  }
