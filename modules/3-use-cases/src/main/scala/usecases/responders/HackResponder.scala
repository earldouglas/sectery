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
    rx.message match

      case "@hack" =>
        summon[Hack[F]]
          .getOrStartGame(rx.service, rx.channel)
          .map { (word, guessCount) =>
            val message =
              s"Guess a word with ${word.length} letters.  ${guessCount} tries so far."
            List(
              Tx(
                service = rx.service,
                channel = rx.channel,
                thread = rx.thread,
                message = message
              )
            )
          }

      case hack(guess) =>
        for
          game <- summon[Hack[F]].getOrStartGame(rx.service, rx.channel)
          (word, guessCount) = game
          found <- summon[Hack[F]].isAWord(guess)
          tx <-
            if guess.length != word.length then
              summon[Monad[F]].pure(
                Tx(
                  service = rx.service,
                  channel = rx.channel,
                  thread = rx.thread,
                  message = s"Guess a word with ${word.length} letters."
                )
              )
            else if found then
              val newGuessCount = guessCount + 1
              val correctCount = countCorrect(guess, word)
              if correctCount == word.length then
                for _ <- summon[Hack[F]]
                    .deleteGame(rx.service, rx.channel)
                yield Tx(
                  service = rx.service,
                  channel = rx.channel,
                  thread = rx.thread,
                  message =
                    s"Guessed ${word} in ${tries(newGuessCount)}."
                )
              else
                for _ <- summon[Hack[F]]
                    .setGuessCount(
                      rx.service,
                      rx.channel,
                      newGuessCount
                    )
                yield
                  val message =
                    s"${correctCount}/${word.length} correct.  ${tries(newGuessCount)} so far."
                  Tx(
                    service = rx.service,
                    channel = rx.channel,
                    thread = rx.thread,
                    message = message
                  )
            else
              summon[Monad[F]].pure(
                Tx(
                  service = rx.service,
                  channel = rx.channel,
                  thread = rx.thread,
                  message = "Guess an actual word."
                )
              )
        yield List(tx)

      case _ =>
        summon[Monad[F]].pure(Nil)
