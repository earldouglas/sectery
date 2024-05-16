package sectery.adaptors

import java.sql.Connection
import sectery._
import sectery.effects._

object LiveHack:

  private def unsafeInit(c: Connection): Unit =
    val s1 =
      """|CREATE TABLE IF NOT EXISTS
         |HACK_WORDS (
         |  WORD VARCHAR(24) NOT NULL
         |)
         |""".stripMargin
    val stmt1 = c.createStatement()
    stmt1.executeUpdate(s1)
    stmt1.close()

    val s2 =
      """|CREATE TABLE IF NOT EXISTS
         |HACK_GAMES (
         |  CHANNEL VARCHAR(256) NOT NULL,
         |  WORD VARCHAR(24) NOT NULL,
         |  COUNT INT NOT NULL,
         |  PRIMARY KEY (CHANNEL)
         |)
         |""".stripMargin
    val stmt2 = c.createStatement()
    stmt2.executeUpdate(s2)
    stmt2.close()

  private def unsafeGetCurrentWord(channel: String)(
      c: Connection
  ): Option[(String, Int)] =
    val s = "SELECT WORD, COUNT FROM HACK_GAMES WHERE CHANNEL = ?"
    val stmt = c.prepareStatement(s)
    stmt.setString(1, channel)
    val rs = stmt.executeQuery
    val wordO =
      if rs.next() then Some((rs.getString("WORD"), rs.getInt("COUNT")))
      else None
    stmt.close
    wordO

  private def unsafeGetRandomWord(c: Connection): String =
    val q = "SELECT WORD FROM HACK_WORDS ORDER BY RAND() LIMIT 1"
    val stmt = c.createStatement
    val rs = stmt.executeQuery(q)
    rs.next()
    val word = rs.getString("WORD")
    stmt.close
    word

  private def unsafeIsAWord(word: String)(c: Connection): Boolean =
    val s =
      "SELECT WORD FROM HACK_WORDS WHERE LOWER(WORD) = ? LIMIT 1"
    val stmt = c.prepareStatement(s)
    stmt.setString(1, word.toLowerCase)
    val rs = stmt.executeQuery
    val found = rs.next()
    stmt.close
    found

  private def unsafeSetCurrentWord(
      channel: String,
      word: String
  )(c: Connection): Unit =
    val s1 = "DELETE FROM HACK_GAMES WHERE CHANNEL = ?"
    val stmt1 = c.prepareStatement(s1)
    stmt1.setString(1, channel)
    stmt1.executeUpdate
    stmt1.close

    val s2 =
      "INSERT INTO HACK_GAMES (CHANNEL, WORD, COUNT) VALUES (?, ?, ?)"
    val stmt2 = c.prepareStatement(s2)
    stmt2.setString(1, channel)
    stmt2.setString(2, word)
    stmt2.setInt(3, 0)
    stmt2.executeUpdate
    stmt2.close

  private def unsafeGetOrStartGame(channel: String)(
      c: Connection
  ): (String, Int) =
    unsafeGetCurrentWord(channel)(c) match
      case Some(wc) =>
        wc
      case None =>
        val word = unsafeGetRandomWord(c)
        unsafeSetCurrentWord(channel, word)(c)
        (word, 0)

  private def unsafeSetGuessCount(
      channel: String,
      guessCount: Int
  )(c: Connection): Unit =
    val s = "UPDATE HACK_GAMES SET COUNT = ? WHERE CHANNEL = ?"
    val stmt = c.prepareStatement(s)
    stmt.setInt(1, guessCount)
    stmt.setString(2, channel)
    stmt.executeUpdate
    stmt.close

  private def unsafeDeleteGame(channel: String)(c: Connection): Unit =
    val s = "DELETE FROM HACK_GAMES WHERE CHANNEL = ?"
    val stmt = c.prepareStatement(s)
    stmt.setString(1, channel)
    stmt.executeUpdate
    stmt.close

  def apply[F[_]: Transactor](c: Connection): Hack[F] =

    unsafeInit(c)

    new Hack:

      override def getOrStartGame(channel: String): F[(String, Int)] =
        summon[Transactor[F]].transact { c =>
          unsafeGetOrStartGame(channel)(c)
        }

      override def isAWord(word: String): F[Boolean] =
        summon[Transactor[F]].transact { c =>
          unsafeIsAWord(word)(c)
        }

      override def deleteGame(channel: String): F[Unit] =
        summon[Transactor[F]].transact { c =>
          unsafeDeleteGame(channel)(c)
        }

      override def setGuessCount(
          channel: String,
          guessCount: Int
      ): F[Unit] =
        summon[Transactor[F]].transact { c =>
          unsafeSetGuessCount(channel, guessCount)(c)
        }
