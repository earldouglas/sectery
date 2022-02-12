package sectery.producers

import java.sql.Connection
import sectery.Db
import sectery.Producer
import sectery.Rx
import sectery.Tx
import zio.ZIO

object Hack extends Producer:

  private val hack = """^@hack\s+([^\s]+)\s*$""".r

  override def help(): Iterable[Info] =
    Some(Info("@hack", "@hack [guess]"))

  override def init(): ZIO[Db.Db, Throwable, Unit] =
    for
      _ <- Db { conn =>
        val s =
          """|CREATE TABLE IF NOT EXISTS
               |HACK_WORDS (
               |  WORD VARCHAR(24) NOT NULL
               |)
               |""".stripMargin
        val stmt = conn.createStatement
        stmt.executeUpdate(s)
        stmt.close
      }
      _ <- Db { conn =>
        val s =
          """|CREATE TABLE IF NOT EXISTS
               |HACK_GAMES (
               |  CHANNEL VARCHAR(256) NOT NULL,
               |  WORD VARCHAR(24) NOT NULL,
               |  COUNT INT NOT NULL,
               |  PRIMARY KEY (CHANNEL)
               |)
               |""".stripMargin
        val stmt = conn.createStatement
        stmt.executeUpdate(s)
        stmt.close
      }
    yield ()

  private def getCurrentWord(
      channel: String
  ): ZIO[Db.Db, Throwable, Option[(String, Int)]] =
    Db { conn =>
      val s = "SELECT WORD, COUNT FROM HACK_GAMES WHERE CHANNEL = ?"
      val stmt = conn.prepareStatement(s)
      stmt.setString(1, channel)
      val rs = stmt.executeQuery
      val wordO =
        if rs.next() then
          Some((rs.getString("WORD"), rs.getInt("COUNT")))
        else None
      stmt.close
      wordO
    }

  private val getRandomWord: ZIO[Db.Db, Throwable, String] =
    Db { conn =>
      val q = "SELECT WORD FROM HACK_WORDS ORDER BY RAND() LIMIT 1"
      val stmt = conn.createStatement
      val rs = stmt.executeQuery(q)
      rs.next()
      val word = rs.getString("WORD")
      stmt.close
      word
    }

  private def isAWord(word: String): ZIO[Db.Db, Throwable, Boolean] =
    Db { conn =>
      val s =
        "SELECT WORD FROM HACK_WORDS WHERE LOWER(WORD) = ? LIMIT 1"
      val stmt = conn.prepareStatement(s)
      stmt.setString(1, word.toLowerCase)
      val rs = stmt.executeQuery
      val found = rs.next()
      stmt.close
      found
    }

  private def setCurrentWord(
      channel: String,
      word: String
  ): ZIO[Db.Db, Throwable, Unit] =
    for
      _ <- Db { conn =>
        val s = "DELETE FROM HACK_GAMES WHERE CHANNEL = ?"
        val stmt = conn.prepareStatement(s)
        stmt.setString(1, channel)
        stmt.executeUpdate
        stmt.close
      }
      _ <- Db { conn =>
        val s =
          "INSERT INTO HACK_GAMES (CHANNEL, WORD, COUNT) VALUES (?, ?, ?)"
        val stmt = conn.prepareStatement(s)
        stmt.setString(1, channel)
        stmt.setString(2, word)
        stmt.setInt(3, 0)
        stmt.executeUpdate
        stmt.close
      }
    yield ()

  private def getOrStartGame(
      channel: String
  ): ZIO[Db.Db, Throwable, (String, Int)] =
    for
      wcO <- getCurrentWord(channel)
      wc <- wcO match
        case Some(wc) => ZIO.succeed(wc)
        case None =>
          for
            word <- getRandomWord
            _ <- setCurrentWord(channel, word)
          yield (word, 0)
    yield wc

  private def countCorrect(guess: String, word: String): Int =
    guess.zip(word).foldLeft(0) { case (z, (ca, cb)) =>
      if ca.toLower == cb.toLower then z + 1 else z
    }

  private def setGuessCount(
      channel: String,
      guessCount: Int
  ): ZIO[Db.Db, Throwable, Unit] =
    Db { conn =>
      val s = "UPDATE HACK_GAMES SET COUNT = ? WHERE CHANNEL = ?"
      val stmt = conn.prepareStatement(s)
      stmt.setInt(1, guessCount)
      stmt.setString(2, channel)
      stmt.executeUpdate
      stmt.close
    }

  private def deleteGame(channel: String): ZIO[Db.Db, Throwable, Unit] =
    Db { conn =>
      val s = "DELETE FROM HACK_GAMES WHERE CHANNEL = ?"
      val stmt = conn.prepareStatement(s)
      stmt.setString(1, channel)
      stmt.executeUpdate
      stmt.close
    }

  private def tries(count: Int): String =
    if count == 1 then s"${count} try" else s"${count} tries"

  override def apply(m: Rx): ZIO[Db.Db, Throwable, Iterable[Tx]] =
    m match
      case Rx(c, _, "@hack") =>
        for (word, guessCount) <- getOrStartGame(c)
        yield Some(Tx(c, s"Guess a word with ${word.length} letters."))
      case Rx(c, _, hack(guess)) =>
        for
          (word, guessCount) <- getOrStartGame(c)
          found <- isAWord(guess)
          tx <-
            if guess.length != word.length then
              ZIO.succeed(
                Tx(c, s"Guess a word with ${word.length} letters.")
              )
            else if found then
              val newGuessCount = guessCount + 1
              val correctCount = countCorrect(guess, word)
              if correctCount == word.length then
                for _ <- deleteGame(c)
                yield Tx(
                  c,
                  s"Guessed ${word} in ${tries(newGuessCount)}."
                )
              else
                for _ <- setGuessCount(c, newGuessCount)
                yield Tx(
                  c,
                  s"${correctCount}/${word.length} correct.  ${tries(newGuessCount)} so far."
                )
            else ZIO.succeed(Tx(c, "Guess an actual word."))
        yield Some(tx)
      case _ =>
        ZIO.succeed(None)
