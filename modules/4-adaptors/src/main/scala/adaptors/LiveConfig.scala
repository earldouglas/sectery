package sectery.adaptors

import java.sql.Connection
import sectery._
import sectery.effects._

object LiveConfig:

  private def unsafeInit(c: Connection): Unit =
    val s =
      """|CREATE TABLE IF NOT EXISTS
         |`CONFIG`(
         |  `NICK` VARCHAR(256) NOT NULL,
         |  `KEY` VARCHAR(256) NOT NULL,
         |  `VALUE` VARCHAR(256) NOT NULL,
         |  PRIMARY KEY (`NICK`, `KEY`)
         |)
         |""".stripMargin
    val stmt = c.createStatement
    stmt.executeUpdate(s)
    stmt.close

  private def unsafeGetConfig(
      nick: String,
      key: String
  )(c: Connection): Option[String] =
    val s =
      """|SELECT `VALUE`
         |FROM `CONFIG`
         |WHERE `NICK` = ?
         |  AND `KEY` = ?
         |""".stripMargin
    val stmt = c.prepareStatement(s)
    stmt.setString(1, nick)
    stmt.setString(2, key)
    val rs = stmt.executeQuery
    var vo: Option[String] = None
    if rs.next() then
      val value = rs.getString("VALUE")
      vo = Some(value)
    stmt.close
    vo

  private def unsafeDeleteValue(
      nick: String,
      key: String
  )(c: Connection): Unit =
    val s =
      """|DELETE FROM `CONFIG`
         |WHERE `NICK` = ?
         |  AND `KEY` = ?
         |""".stripMargin
    val stmt = c.prepareStatement(s)
    stmt.setString(1, nick)
    stmt.setString(2, key)
    stmt.executeUpdate
    stmt.close

  private def unsafeInsertValue(
      nick: String,
      key: String,
      value: String
  )(c: Connection): Unit =
    val s =
      """|INSERT INTO `CONFIG`
         |(`NICK`, `KEY`, `VALUE`)
         |VALUES (?, ?, ?)
         |""".stripMargin
    val stmt = c.prepareStatement(s)
    stmt.setString(1, nick)
    stmt.setString(2, key)
    stmt.setString(3, value)
    stmt.executeUpdate
    stmt.close

  def apply[F[_]: Transactor](
      c: Connection
  ): GetConfig[F] & SetConfig[F] =

    unsafeInit(c)

    new GetConfig[F] with SetConfig[F]:

      override def setConfig(
          nick: String,
          key: String,
          value: String
      ): F[Unit] =
        summon[Transactor[F]].transact { c =>
          unsafeDeleteValue(nick, key)(c)
          unsafeInsertValue(nick, key, value)(c)
        }

      override def getConfig(
          nick: String,
          key: String
      ): F[Option[String]] =
        summon[Transactor[F]].transact { c =>
          unsafeGetConfig(nick, key)(c)
        }
