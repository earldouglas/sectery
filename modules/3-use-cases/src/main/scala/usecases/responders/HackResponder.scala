package sectery.usecases.responders

import sectery.control.Monad
import sectery.control.Monad._
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases.Responder

class HackResponder[F[_]: Monad: Hack] extends Responder[F]:

  override def name = "@hack"

  override def usage = "@hack"

  private val hack = """^@hack\s+([^\s]+)\s*$""".r

  private def countCorrect(guess: String, word: String): Int =
    guess.zip(word).foldLeft(0) { case (z, (ca, cb)) =>
      if ca.toLower == cb.toLower then z + 1 else z
    }

  private def tries(count: Int): String =
    if count == 1 then s"${count} try" else s"${count} tries"

  override def respondToMessage(rx: Rx) =
    rx match

      case Rx(c, _, "@hack") =>
        summon[Hack[F]]
          .getOrStartGame(c)
          .map { (word, guessCount) =>
            val message =
              s"Guess a word with ${word.length} letters.  ${guessCount} tries so far."
            List(Tx(c, message))
          }

      case Rx(c, _, hack(guess)) =>
        for
          game <- summon[Hack[F]].getOrStartGame(c)
          (word, guessCount) = game
          found <- summon[Hack[F]].isAWord(guess)
          tx <-
            if guess.length != word.length then
              summon[Monad[F]].pure(
                Tx(c, s"Guess a word with ${word.length} letters.")
              )
            else if found then
              val newGuessCount = guessCount + 1
              val correctCount = countCorrect(guess, word)
              if correctCount == word.length then
                for _ <- summon[Hack[F]].deleteGame(c)
                yield Tx(
                  c,
                  s"Guessed ${word} in ${tries(newGuessCount)}."
                )
              else
                for _ <- summon[Hack[F]].setGuessCount(c, newGuessCount)
                yield
                  val message =
                    s"${correctCount}/${word.length} correct.  ${tries(newGuessCount)} so far."
                  Tx(c, message)
            else summon[Monad[F]].pure(Tx(c, "Guess an actual word."))
        yield List(tx)

      case _ =>
        summon[Monad[F]].pure(Nil)
