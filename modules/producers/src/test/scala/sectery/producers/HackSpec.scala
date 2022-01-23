package sectery.producers

import sectery._
import zio.Inject._

object HackSpec extends ProducerSpec:

  private def insert(word: String): ZStep =
    Db { conn =>
      val s = "INSERT INTO HACK_WORDS (WORD) VALUES (?)"
      val stmt = conn.prepareStatement(s)
      stmt.setString(1, word)
      stmt.executeUpdate
      stmt.close
    }

  override val specs =
    Map(
      "@hack flow" ->
        (
          List(
            insert("yea"),
            Rx("#foo", "bar", "@hack"), // start a new game
            insert("yes"),
            insert("yeah"),
            Rx("#foo", "bar", "@hack foo"), // guess a non-word (+0)
            Rx("#foo", "bar", "@hack yeah"), // guess a long word (+0)
            Rx("#foo", "bar", "@hack yes"), // guess a wrong word (+1)
            Rx("#foo", "bar", "@hack"), // report status (+0)
            Rx("#foo", "bar", "@hack yea") // guess correct word (+1)
          ),
          List(
            Tx("#foo", "Guess a word with 3 letters."),
            Tx("#foo", "Guess an actual word."),
            Tx("#foo", "Guess a word with 3 letters."),
            Tx("#foo", "2/3 correct.  1 try so far."),
            Tx("#foo", "Guess a word with 3 letters."),
            Tx("#foo", "Guessed yea in 2 tries.")
          )
        )
    )
