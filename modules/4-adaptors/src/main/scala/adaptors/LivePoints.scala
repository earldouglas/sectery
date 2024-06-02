package sectery.adaptors

import java.sql.Connection
import sectery.effects.Points
import sectery.effects.Transactor

object LivePoints:

  private def unsafeInit(c: Connection): Unit =
    val s =
      """|CREATE TABLE IF NOT EXISTS
         |`POINTS_V2`(
         |  `SERVICE` VARCHAR(256) NOT NULL,
         |  `CHANNEL` VARCHAR(256) NOT NULL,
         |  `NICK` VARCHAR(256) NOT NULL,
         |  `VALUE` INT NOT NULL,
         |  PRIMARY KEY (`SERVICE`, `CHANNEL`, `NICK`)
         |)
         |""".stripMargin
    val stmt = c.createStatement
    stmt.executeUpdate(s)
    stmt.close

  private def unsafeUpdateValue(
      c: Connection,
      service: String,
      channel: String,
      nick: String,
      delta: Int
  ): Int =

    def getOldValue(c: Connection): Int =
      val s =
        """|SELECT `VALUE` FROM `POINTS_V2`
           |WHERE `SERVICE` = ?
           |  AND `CHANNEL` = ?
           |  AND `NICK` = ?
           |LIMIT 1
           |""".stripMargin
      val stmt = c.prepareStatement(s)
      stmt.setString(1, service)
      stmt.setString(2, channel)
      stmt.setString(3, nick)
      val rs = stmt.executeQuery()
      val oldValue =
        if rs.next() then rs.getInt("VALUE")
        else 0
      stmt.close
      oldValue

    def dropValue(c: Connection): Unit =
      val s =
        """|DELETE FROM `POINTS_V2`
           |WHERE `SERVICE` = ?
           |  AND `CHANNEL` = ?
           |  AND `NICK` = ?
           |""".stripMargin
      val stmt = c.prepareStatement(s)
      stmt.setString(1, service)
      stmt.setString(2, channel)
      stmt.setString(3, nick)
      stmt.executeUpdate()
      stmt.close

    def setNewValue(c: Connection, newValue: Int): Int =
      val s =
        """|INSERT INTO `POINTS_V2` (`SERVICE`, `CHANNEL`, `NICK`, `VALUE`)
           |VALUES (?, ?, ?, ?)
           |""".stripMargin
      val stmt = c.prepareStatement(s)
      stmt.setString(1, service)
      stmt.setString(2, channel)
      stmt.setString(3, nick)
      stmt.setInt(4, newValue)
      stmt.executeUpdate()
      stmt.close
      newValue

    val oldValue = getOldValue(c)
    dropValue(c)
    val newValue = oldValue + delta
    setNewValue(c, newValue)

  def apply[F[_]: Transactor](c: Connection): Points[F] =

    unsafeInit(c)

    new Points:
      override def update(
          service: String,
          channel: String,
          nick: String,
          delta: Int
      ): F[Long] =
        summon[Transactor[F]].transact { c =>
          unsafeUpdateValue(
            c,
            service = service,
            channel = channel,
            nick = nick,
            delta = delta
          )
        }
