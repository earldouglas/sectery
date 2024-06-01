package sectery.usecases.responders

import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class KryptoResponderSuite extends FunSuite:

  trait KryptoStub extends Krypto[Id]:

    override def getOrStartGame(service: String, channel: String) = ???

    override def deleteGame(service: String, channel: String) = ???

    override def setGuessCount(
        service: String,
        channel: String,
        guessCount: Int
    ) = ???

  test("@krypto: start a new game") {

    given krypto: Krypto[Id] =
      new KryptoStub:
        override def getOrStartGame(service: String, channel: String) =
          (service, channel) match
            case ("irc", "#foo") =>
              Krypto.Game(
                guessCount = 0,
                objective = 8,
                cards = (9, 16, 1, 3, 18)
              )

    assertEquals(
      new KryptoResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@krypto")
        ),
      List(
        Tx(
          "irc",
          "#foo",
          None,
          "Cards: [9, 16, 1, 3, 18].  Objective: 8.  0 tries so far."
        )
      )
    )
  }

  test(
    "@krypto: guess an incorrect solution and increment the guess count"
  ) {

    var _guessCount = 0

    given krypto: Krypto[Id] =
      new KryptoStub:

        override def setGuessCount(
            service: String,
            channel: String,
            guessCount: Int
        ) =
          (service, channel, guessCount) match
            case ("irc", "#foo", 1) =>
              _guessCount = guessCount

        override def getOrStartGame(service: String, channel: String) =
          (service, channel) match
            case ("irc", "#foo") =>
              Krypto.Game(
                guessCount = _guessCount,
                objective = 8,
                cards = (9, 16, 1, 3, 18)
              )

    assertEquals(
      obtained = new KryptoResponder[Id]
        .respondToMessage(
          Rx(
            "irc",
            "#foo",
            None,
            "bar",
            "@krypto 9 * 16 + 1 / (18 - 3)"
          )
        ),
      expected = List(
        Tx(
          "irc",
          "#foo",
          None,
          "Try again.  1 try so far."
        )
      )
    )

    assertEquals(
      obtained = _guessCount,
      expected = 1
    )
  }

  test("@krypto: guess a correct solution and delete the game") {

    var deleted: Boolean = false

    given krypto: Krypto[Id] =
      new KryptoStub:

        override def deleteGame(service: String, channel: String) =
          deleted = true

        override def setGuessCount(
            service: String,
            channel: String,
            guessCount: Int
        ) = ()

        override def getOrStartGame(service: String, channel: String) =
          (service, channel) match
            case ("irc", "#foo") =>
              Krypto.Game(
                guessCount = 0,
                objective = 8,
                cards = (9, 16, 1, 3, 18)
              )

    assertEquals(
      obtained = new KryptoResponder[Id]
        .respondToMessage(
          Rx(
            "irc",
            "#foo",
            None,
            "bar",
            "@krypto 9 - 16 + 1 * (18 - 3)"
          )
        ),
      expected = List(
        Tx(
          "irc",
          "#foo",
          None,
          "Krypto!  Got it in 1 try."
        )
      )
    )

    assert(deleted)
  }

  test("@krypto: guess with the wrong cards") {
    given krypto: Krypto[Id] =
      new KryptoStub:
        override def getOrStartGame(service: String, channel: String) =
          (service, channel) match
            case ("irc", "#foo") =>
              Krypto.Game(
                guessCount = 0,
                objective = 8,
                cards = (9, 16, 1, 3, 18)
              )

    assertEquals(
      obtained = new KryptoResponder[Id]
        .respondToMessage(
          Rx(
            "irc",
            "#foo",
            None,
            "bar",
            "@krypto 8 - 16 + 1 * (18 - 3)"
          )
        ),
      expected = List(
        Tx(
          "irc",
          "#foo",
          None,
          "Gotta play the cards as dealt."
        )
      )
    )
  }

  test("@krypto: guess a non-parseable solution") {
    given krypto: Krypto[Id] =
      new KryptoStub:
        override def getOrStartGame(service: String, channel: String) =
          (service, channel) match
            case ("irc", "#foo") =>
              Krypto.Game(
                guessCount = 0,
                objective = 8,
                cards = (9, 16, 1, 3, 18)
              )

    assertEquals(
      obtained = new KryptoResponder[Id]
        .respondToMessage(
          Rx(
            "irc",
            "#foo",
            None,
            "bar",
            "@krypto (2 + 3) * **2 ** 1 * 1*"
          )
        ),
      expected = List(
        Tx("irc", "#foo", None, "Can't parse that.")
      )
    )
  }
