package sectery.usecases.responders

import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class KryptoResponderSuite extends FunSuite:

  trait KryptoStub extends Krypto[Id]:
    override def getOrStartGame(channel: String) = ???
    override def deleteGame(channel: String) = ???
    override def setGuessCount(channel: String, guessCount: Int) = ???

  test("@krypto: start a new game") {

    given krypto: Krypto[Id] =
      new KryptoStub:
        override def getOrStartGame(channel: String) =
          channel match
            case "#foo" =>
              Krypto.Game(
                guessCount = 0,
                objective = 8,
                cards = (9, 16, 1, 3, 18)
              )

    assertEquals(
      new KryptoResponder[Id]
        .respondToMessage(Rx("#foo", "bar", "@krypto")),
      List(
        Tx(
          "#foo",
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
        override def deleteGame(channel: String) = ???
        override def setGuessCount(channel: String, guessCount: Int) =
          (channel, guessCount) match
            case ("#foo", 1) =>
              _guessCount = guessCount
        override def getOrStartGame(channel: String) =
          channel match
            case "#foo" =>
              Krypto.Game(
                guessCount = _guessCount,
                objective = 8,
                cards = (9, 16, 1, 3, 18)
              )

    assertEquals(
      obtained = new KryptoResponder[Id]
        .respondToMessage(
          Rx("#foo", "bar", "@krypto 9 * 16 + 1 * (18 - 3)")
        ),
      expected = List(Tx("#foo", "Try again.  1 try so far."))
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
        override def deleteGame(channel: String) =
          deleted = true
        override def setGuessCount(channel: String, guessCount: Int) =
          ()
        override def getOrStartGame(channel: String) =
          channel match
            case "#foo" =>
              Krypto.Game(
                guessCount = 0,
                objective = 8,
                cards = (9, 16, 1, 3, 18)
              )

    assertEquals(
      obtained = new KryptoResponder[Id]
        .respondToMessage(
          Rx("#foo", "bar", "@krypto 9 - 16 + 1 * (18 - 3)")
        ),
      expected = List(Tx("#foo", "Krypto!  Got it in 1 try."))
    )

    assert(deleted)
  }

  test("@krypto: guess a non-parseable solution") {
    given krypto: Krypto[Id] =
      new KryptoStub:
        override def deleteGame(channel: String) = ???
        override def setGuessCount(channel: String, guessCount: Int) =
          ???
        override def getOrStartGame(channel: String) =
          channel match
            case "#foo" =>
              Krypto.Game(
                guessCount = 0,
                objective = 8,
                cards = (9, 16, 1, 3, 18)
              )
    assertEquals(
      obtained = new KryptoResponder[Id]
        .respondToMessage(
          Rx("#foo", "bar", "@krypto purple monkey dishwasher")
        ),
      expected = List(Tx("#foo", "Can't parse that."))
    )
  }
