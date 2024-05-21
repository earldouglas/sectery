package sectery.adaptors

import java.sql.Connection
import sectery._
import sectery.effects._

object LiveKrypto:

  private def unsafeInit(c: Connection): Unit =
    val s =
      """|CREATE TABLE IF NOT EXISTS
         |KRYPTO (
         |  CHANNEL VARCHAR(256) NOT NULL,
         |  OBJECTIVE INT NOT NULL,
         |  CARD1 INT NOT NULL,
         |  CARD2 INT NOT NULL,
         |  CARD3 INT NOT NULL,
         |  CARD4 INT NOT NULL,
         |  CARD5 INT NOT NULL,
         |  COUNT INT NOT NULL,
         |  PRIMARY KEY (CHANNEL)
         |)
         |""".stripMargin
    val stmt = c.createStatement()
    stmt.executeUpdate(s)
    stmt.close()

  private def unsafeGetCurrentGame(channel: String)(
      c: Connection
  ): Option[Krypto.Game] =
    val s =
      "SELECT OBJECTIVE, CARD1, CARD2, CARD3, CARD4, CARD5, COUNT FROM KRYPTO WHERE CHANNEL = ?"
    val stmt = c.prepareStatement(s)
    stmt.setString(1, channel)
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

  private def unsafeSetCurrentGame(channel: String, game: Krypto.Game)(
      c: Connection
  ): Unit =

    unsafeDeleteGame(channel)(c)

    val s =
      "INSERT INTO KRYPTO (CHANNEL, OBJECTIVE, CARD1, CARD2, CARD3, CARD4, CARD5, COUNT) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
    val stmt = c.prepareStatement(s)
    stmt.setString(1, channel)
    stmt.setInt(2, game.objective)
    stmt.setInt(3, game.cards._1)
    stmt.setInt(4, game.cards._2)
    stmt.setInt(5, game.cards._3)
    stmt.setInt(6, game.cards._4)
    stmt.setInt(7, game.cards._5)
    stmt.setInt(8, game.guessCount)
    stmt.executeUpdate
    stmt.close

  private def unsafeGetOrStartGame(channel: String)(
      c: Connection
  ): Krypto.Game =
    unsafeGetCurrentGame(channel)(c) match
      case Some(game) =>
        game
      case None =>
        val game = Krypto.newGame
        unsafeSetCurrentGame(channel, game)(c)
        game

  private def unsafeSetGuessCount(
      channel: String,
      guessCount: Int
  )(c: Connection): Unit =
    val s = "UPDATE KRYPTO SET COUNT = ? WHERE CHANNEL = ?"
    val stmt = c.prepareStatement(s)
    stmt.setInt(1, guessCount)
    stmt.setString(2, channel)
    stmt.executeUpdate
    stmt.close

  private def unsafeDeleteGame(channel: String)(c: Connection): Unit =
    val s = "DELETE FROM KRYPTO WHERE CHANNEL = ?"
    val stmt = c.prepareStatement(s)
    stmt.setString(1, channel)
    stmt.executeUpdate
    stmt.close

  def apply[F[_]: Transactor](c: Connection): Krypto[F] =

    unsafeInit(c)

    new Krypto:

      override def getOrStartGame(channel: String): F[Krypto.Game] =
        summon[Transactor[F]].transact { c =>
          unsafeGetOrStartGame(channel)(c)
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
