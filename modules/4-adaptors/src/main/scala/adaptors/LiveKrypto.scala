package sectery.adaptors

import java.sql.Connection
import sectery._
import sectery.effects._

object LiveKrypto:

  private def unsafeInit(c: Connection): Unit =
    val s =
      """|CREATE TABLE IF NOT EXISTS
         |`KRYPTO_V2` (
         |  `SERVICE` VARCHAR(256) NOT NULL,
         |  `CHANNEL` VARCHAR(256) NOT NULL,
         |  `OBJECTIVE` INT NOT NULL,
         |  `CARD1` INT NOT NULL,
         |  `CARD2` INT NOT NULL,
         |  `CARD3` INT NOT NULL,
         |  `CARD4` INT NOT NULL,
         |  `CARD5` INT NOT NULL,
         |  `COUNT` INT NOT NULL,
         |  PRIMARY KEY (`SERVICE`, `CHANNEL`)
         |)
         |""".stripMargin
    val stmt = c.createStatement()
    stmt.executeUpdate(s)
    stmt.close()

  private def unsafeGetCurrentGame(
      c: Connection,
      service: String,
      channel: String
  ): Option[Krypto.Game] =
    val s =
      """|SELECT *
         |FROM `KRYPTO_V2`
         |WHERE `SERVICE` = ?
         |  AND `CHANNEL` = ?
         |""".stripMargin
    val stmt = c.prepareStatement(s)
    stmt.setString(1, service)
    stmt.setString(2, channel)
    val rs = stmt.executeQuery
    val gameO =
      if rs.next() then
        Some(
          Krypto.Game(
            guessCount = rs.getInt("COUNT"),
            objective = rs.getInt("OBJECTIVE"),
            cards = (
              rs.getInt("CARD1"),
              rs.getInt("CARD2"),
              rs.getInt("CARD3"),
              rs.getInt("CARD4"),
              rs.getInt("CARD5")
            )
          )
        )
      else None
    stmt.close
    gameO

  private def unsafeDeleteGame(
      c: Connection,
      service: String,
      channel: String
  ): Unit =
    val s =
      """|DELETE FROM `KRYPTO_V2`
         |WHERE `SERVICE` = ?
         |  AND `CHANNEL` = ?
         |""".stripMargin
    val stmt = c.prepareStatement(s)
    stmt.setString(1, service)
    stmt.setString(2, channel)
    stmt.executeUpdate
    stmt.close

  private def unsafeSetCurrentGame(
      c: Connection,
      service: String,
      channel: String,
      game: Krypto.Game
  ): Unit =

    unsafeDeleteGame(c, service, channel)

    val s =
      """|INSERT INTO `KRYPTO_V2` (`SERVICE`, `CHANNEL`, `OBJECTIVE`, `CARD1`, `CARD2`, `CARD3`, `CARD4`, `CARD5`, `COUNT`)
         |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
         |""".stripMargin
    val stmt = c.prepareStatement(s)
    stmt.setString(1, service)
    stmt.setString(2, channel)
    stmt.setInt(3, game.objective)
    stmt.setInt(4, game.cards._1)
    stmt.setInt(5, game.cards._2)
    stmt.setInt(6, game.cards._3)
    stmt.setInt(7, game.cards._4)
    stmt.setInt(8, game.cards._5)
    stmt.setInt(9, game.guessCount)
    stmt.executeUpdate
    stmt.close

  private def unsafeGetOrStartGame(
      c: Connection,
      service: String,
      channel: String
  ): Krypto.Game =
    unsafeGetCurrentGame(c, service, channel) match
      case Some(game) =>
        game
      case None =>
        val game = Krypto.newGame
        unsafeSetCurrentGame(c, service, channel, game)
        game

  private def unsafeSetGuessCount(
      c: Connection,
      service: String,
      channel: String,
      guessCount: Int
  ): Unit =
    val s =
      """|UPDATE `KRYPTO_V2`
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

  def apply[F[_]: Transactor](c: Connection): Krypto[F] =

    unsafeInit(c)

    new Krypto:

      override def getOrStartGame(
          service: String,
          channel: String
      ): F[Krypto.Game] =
        summon[Transactor[F]].transact { c =>
          unsafeGetOrStartGame(c, service, channel)
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
