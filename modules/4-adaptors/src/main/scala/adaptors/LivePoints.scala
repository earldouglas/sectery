package sectery.adaptors

import java.sql.Connection
import sectery.effects.Points
import sectery.effects.Transactor

object LivePoints:

  private def unsafeInit(c: Connection): Unit =
    val s =
      """|CREATE TABLE IF NOT EXISTS
         |`POINTS`(
         |  `CHANNEL` VARCHAR(256) NOT NULL,
         |  `NICK` VARCHAR(256) NOT NULL,
         |  `VALUE` INT NOT NULL
         |)
         |""".stripMargin
    val stmt = c.createStatement
    stmt.executeUpdate(s)
    stmt.close

  private def unsafeUpdateValue(
      channel: String,
      nick: String,
      delta: Int
  )(c: Connection): Int =

    def getOldValue(c: Connection): Int =
      val s =
        """|SELECT `VALUE` FROM `POINTS`
           |WHERE `CHANNEL` = ?
           |AND `NICK` = ?
           |LIMIT 1
           |""".stripMargin
      val stmt = c.prepareStatement(s)
      stmt.setString(1, channel)
      stmt.setString(2, nick)
      val rs = stmt.executeQuery()
      val oldValue =
        if rs.next() then rs.getInt("VALUE")
        else 0
      stmt.close
      oldValue

    def dropValue(c: Connection): Unit =
      val s =
        """|DELETE FROM `POINTS`
           |WHERE `CHANNEL` = ?
           |AND `NICK` = ?
           |""".stripMargin
      val stmt = c.prepareStatement(s)
      stmt.setString(1, channel)
      stmt.setString(2, nick)
      stmt.executeUpdate()
      stmt.close

    def setNewValue(newValue: Int)(c: Connection): Int =
      val s =
        """|INSERT INTO `POINTS` (`CHANNEL`, `NICK`, `VALUE`)
           |VALUES (?, ?, ?)
           |""".stripMargin
      val stmt = c.prepareStatement(s)
      stmt.setString(1, channel)
      stmt.setString(2, nick)
      stmt.setInt(3, newValue)
      stmt.executeUpdate()
      stmt.close
      newValue

    val oldValue = getOldValue(c)
    dropValue(c)
    val newValue = oldValue + delta
    setNewValue(newValue)(c)

  def apply[F[_]: Transactor](c: Connection): Points[F] =

    unsafeInit(c)

    new Points:
      override def update(
          channel: String,
          nick: String,
          delta: Int
      ): F[Long] =
        summon[Transactor[F]].transact { c =>
          unsafeUpdateValue(
            channel = channel,
            nick = nick,
            delta = delta
          )(c)
        }
