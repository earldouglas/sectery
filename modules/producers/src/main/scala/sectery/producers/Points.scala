package sectery.producers

import java.sql.Connection
import sectery.Db
import sectery.Producer
import sectery.Rx
import sectery.Tx
import zio.ZIO

object Points extends Producer:

  override def help(): Iterable[Info] =
    None

  override def init(): ZIO[Db.Db, Throwable, Unit] =
    for _ <- Db { conn =>
        val s =
          """|CREATE TABLE IF NOT EXISTS
             |`POINTS`(
             |  `CHANNEL` VARCHAR(256) NOT NULL,
             |  `NICK` VARCHAR(256) NOT NULL,
             |  `VALUE` INT NOT NULL
             |)
             |""".stripMargin
        val stmt = conn.createStatement
        stmt.executeUpdate(s)
        stmt.close
      }
    yield ()

  private def updateValue(
      channel: String,
      nick: String,
      delta: Int
  ): ZIO[Db.Db, Throwable, Int] =
    def getOldValue(conn: Connection): Int =
      val s =
        """|SELECT `VALUE` FROM `POINTS`
           |WHERE `CHANNEL` = ?
           |AND `NICK` = ?
           |LIMIT 1
           |""".stripMargin
      val stmt = conn.prepareStatement(s)
      stmt.setString(1, channel)
      stmt.setString(2, nick)
      val rs = stmt.executeQuery()
      val oldValue =
        if rs.next() then rs.getInt("VALUE")
        else 0
      stmt.close
      oldValue

    def dropValue(conn: Connection): Unit =
      val s =
        """|DELETE FROM `POINTS`
           |WHERE `CHANNEL` = ?
           |AND `NICK` = ?
           |""".stripMargin
      val stmt = conn.prepareStatement(s)
      stmt.setString(1, channel)
      stmt.setString(2, nick)
      stmt.executeUpdate()
      stmt.close

    def setNewValue(newValue: Int)(conn: Connection): Int =
      val s =
        """|INSERT INTO `POINTS` (`CHANNEL`, `NICK`, `VALUE`)
           |VALUES (?, ?, ?)
           |""".stripMargin
      val stmt = conn.prepareStatement(s)
      stmt.setString(1, channel)
      stmt.setString(2, nick)
      stmt.setInt(3, newValue)
      stmt.executeUpdate()
      stmt.close
      newValue

    Db { conn =>
      val oldValue = getOldValue(conn)
      dropValue(conn)
      val newValue = oldValue + delta
      setNewValue(newValue)(conn)
    }

  private val plusOne = """^\s*([^+]+)[+][+]\s*$""".r
  private val minusOne = """^\s*([^+]+)[-][-]\s*$""".r

  override def apply(m: Rx): ZIO[Db.Db, Throwable, Iterable[Tx]] =
    m match

      case Rx(channel, fromNick, plusOne(plusOneNick))
          if fromNick == plusOneNick =>
        ZIO.succeed(
          Some(Tx(channel, "You gotta get someone else to do it."))
        )

      case Rx(channel, fromNick, plusOne(plusOneNick))
          if fromNick != plusOneNick =>
        for newValue <- updateValue(channel, plusOneNick, 1)
        yield Some(
          Tx(
            channel,
            s"""${plusOneNick} has ${newValue} point${
                if newValue == 1 then "" else "s"
              }."""
          )
        )

      case Rx(channel, fromNick, minusOne(minusOneNick))
          if fromNick == minusOneNick =>
        ZIO.succeed(
          Some(Tx(channel, "You gotta get someone else to do it."))
        )

      case Rx(channel, fromNick, minusOne(minusOneNick))
          if fromNick != minusOneNick =>
        for newValue <- updateValue(channel, minusOneNick, -1)
        yield Some(
          Tx(
            channel,
            s"""${minusOneNick} has ${newValue} point${
                if newValue == 1 then "" else "s"
              }."""
          )
        )

      case _ =>
        ZIO.succeed(None)
