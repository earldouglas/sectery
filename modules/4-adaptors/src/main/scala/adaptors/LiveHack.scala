package sectery.adaptors

import java.sql.Connection
import sectery._
import sectery.effects._

object LiveHack:

  private def unsafeInit(c: Connection): Unit =
    val s1 =
      """|CREATE TABLE IF NOT EXISTS
         |`HACK_WORDS` (
         |  `WORD` VARCHAR(24) NOT NULL
         |)
         |""".stripMargin
    val stmt1 = c.createStatement()
    stmt1.executeUpdate(s1)
    stmt1.close()

    val s2 =
      """|CREATE TABLE IF NOT EXISTS
         |`HACK_GAMES_V2` (
         |  `SERVICE` VARCHAR(256) NOT NULL,
         |  `CHANNEL` VARCHAR(256) NOT NULL,
         |  `WORD` VARCHAR(24) NOT NULL,
         |  `COUNT` INT NOT NULL,
         |  PRIMARY KEY (`SERVICE`, `CHANNEL`)
         |)
         |""".stripMargin
    val stmt2 = c.createStatement()
    stmt2.executeUpdate(s2)
    stmt2.close()

  private def unsafeGetCurrentWord(
      c: Connection,
      service: String,
      channel: String
  ): Option[(String, Int)] =
    val s =
      """|SELECT `WORD`, `COUNT`
         |FROM `HACK_GAMES_V2`
         |WHERE `SERVICE` = ?
         |  AND `CHANNEL` = ?
         |""".stripMargin
    val stmt = c.prepareStatement(s)
    stmt.setString(1, service)
    stmt.setString(2, channel)
    val rs = stmt.executeQuery
    val wordO =
      if rs.next() then Some((rs.getString("WORD"), rs.getInt("COUNT")))
      else None
    stmt.close
    wordO

  private def unsafeGetRandomWord(c: Connection): String =
    val q =
      """|SELECT `WORD`
         |FROM `HACK_WORDS`
         |ORDER BY RAND()
         |LIMIT 1
         |""".stripMargin
    val stmt = c.createStatement
    val rs = stmt.executeQuery(q)
    rs.next()
    val word = rs.getString("WORD")
    stmt.close
    word

  private def unsafeIsAWord(c: Connection, word: String): Boolean =
    val s =
      """|SELECT `WORD`
         |FROM `HACK_WORDS`
         |WHERE LOWER(`WORD`) = ?
         |LIMIT 1
         |""".stripMargin
    val stmt = c.prepareStatement(s)
    stmt.setString(1, word.toLowerCase)
    val rs = stmt.executeQuery
    val found = rs.next()
    stmt.close
    found

  private def unsafeSetCurrentWord(
      c: Connection,
      service: String,
      channel: String,
      word: String
  ): Unit =

    unsafeDeleteGame(c, service, channel)

    val s =
      """|INSERT INTO
         |HACK_GAMES_V2 (`SERVICE`, `CHANNEL`, `WORD`, `COUNT`)
         |VALUES (?, ?, ?, ?)
         |""".stripMargin
    val stmt = c.prepareStatement(s)
    stmt.setString(1, service)
    stmt.setString(2, channel)
    stmt.setString(3, word)
    stmt.setInt(4, 0)
    stmt.executeUpdate
    stmt.close

  private def unsafeGetOrStartGame(
      c: Connection,
      service: String,
      channel: String
  ): (String, Int) =
    unsafeGetCurrentWord(c, service, channel) match
      case Some(wc) =>
        wc
      case None =>
        val word = unsafeGetRandomWord(c)
        unsafeSetCurrentWord(c, service, channel, word)
        (word, 0)

  private def unsafeSetGuessCount(
      c: Connection,
      service: String,
      channel: String,
      guessCount: Int
  ): Unit =
    val s =
      """|UPDATE `HACK_GAMES_V2`
         |SET `COUNT` = ?
         |WHERE `SERVICE` = ?
         |  AND `CHANNEL` = ?
         |""".stripMargin
    val stmt = c.prepareStatement(s)
    stmt.setInt(1, guessCount)
    stmt.setString(2, service)
    stmt.setString(3, channel)
    stmt.executeUpdate
    stmt.close

  private def unsafeDeleteGame(
      c: Connection,
      service: String,
      channel: String
  ): Unit =
    val s =
      """|DELETE FROM
         |`HACK_GAMES_V2`
         |WHERE `SERVICE` = ?
         |  AND `CHANNEL` = ?
         |""".stripMargin
    val stmt = c.prepareStatement(s)
    stmt.setString(1, service)
    stmt.setString(2, channel)
    stmt.executeUpdate
    stmt.close

  def apply[F[_]: Transactor](c: Connection): Hack[F] =

    unsafeInit(c)

    new Hack:

      override def getOrStartGame(
          service: String,
          channel: String
      ): F[(String, Int)] =
        summon[Transactor[F]].transact { c =>
          unsafeGetOrStartGame(c, service, channel)
        }

      override def isAWord(word: String): F[Boolean] =
        summon[Transactor[F]].transact { c =>
          unsafeIsAWord(c, word)
        }

      override def deleteGame(
          service: String,
          channel: String
      ): F[Unit] =
        summon[Transactor[F]].transact { c =>
          unsafeDeleteGame(c, service, channel)
        }

      override def setGuessCount(
          service: String,
          channel: String,
          guessCount: Int
      ): F[Unit] =
        summon[Transactor[F]].transact { c =>
          unsafeSetGuessCount(c, service, channel, guessCount)
        }
