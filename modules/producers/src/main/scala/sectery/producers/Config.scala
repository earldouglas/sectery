package sectery.producers

import java.sql.Connection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import sectery.Db
import sectery.Producer
import sectery.Rx
import sectery.Tx
import zio.Clock
import zio.ZIO

object Config extends Producer:

  private val get = """^@get\s+([^\s]+)\s*$""".r
  private val set = """^@set\s+([^\s]+)\s+(.+)\s*$""".r

  override def help(): Iterable[Info] =
    List(
      Info(
        "@get",
        "@get <key>, e.g. @get tz"
      ),
      Info(
        "@set",
        "@set <key> <value>, e.g. @set tz America/New_York"
      )
    )

  override def init(): ZIO[Db.Db, Throwable, Unit] =
    for
      _ <- Db { conn =>
        val s =
          """|CREATE TABLE IF NOT EXISTS
             |CONFIG(
             |  NICK VARCHAR(256) NOT NULL,
             |  KEY VARCHAR(256) NOT NULL,
             |  VALUE VARCHAR(256) NOT NULL,
             |  PRIMARY KEY (NICK, KEY)
             |)
             |""".stripMargin
        val stmt = conn.createStatement
        stmt.executeUpdate(s)
        stmt.close
      }
    yield ()

  def getConfig(
      nick: String,
      key: String
  ): ZIO[Db.Db, Throwable, Option[String]] =
    Db { conn =>
      val s =
        """|SELECT VALUE
           |FROM CONFIG
           |WHERE NICK = ?
           |  AND KEY = ?
           |""".stripMargin
      val stmt = conn.prepareStatement(s)
      stmt.setString(1, nick)
      stmt.setString(2, key)
      val rs = stmt.executeQuery
      var vo: Option[String] = None
      if rs.next() then
        val value = rs.getString("VALUE")
        vo = Some(value)
      stmt.close
      vo
    }

  private def setConfig(
      nick: String,
      key: String,
      value: String
  ): ZIO[Db.Db, Throwable, Unit] =

    def dropConfig(conn: Connection): Unit =
      val s =
        """|DELETE FROM CONFIG
           |WHERE NICK = ?
           |  AND KEY = ?
           |""".stripMargin
      val stmt = conn.prepareStatement(s)
      stmt.setString(1, nick)
      stmt.setString(2, key)
      stmt.executeUpdate
      stmt.close

    def addConfig(conn: Connection): Unit =
      val s =
        """|INSERT INTO CONFIG (NICK, KEY, VALUE)
           |VALUES (?, ?, ?)
           |""".stripMargin
      val stmt = conn.prepareStatement(s)
      stmt.setString(1, nick)
      stmt.setString(2, key)
      stmt.setString(3, value)
      stmt.executeUpdate
      stmt.close

    Db { conn =>
      dropConfig(conn)
      addConfig(conn)
    }

  override def apply(m: Rx): ZIO[Db.Db, Throwable, Iterable[Tx]] =
    m match
      case Rx(c, nick, get(key)) =>
        getConfig(nick, key).map {
          _ match
            case Some(value) =>
              Some(Tx(c, s"${nick}: ${key} is set to ${value}"))
            case None =>
              Some(Tx(c, s"${nick}: ${key} is not set"))
        }
      case Rx(c, nick, set(key, value)) =>
        for _ <- setConfig(nick, key, value)
        yield Some(Tx(c, s"${nick}: ${key} set to ${value}"))
      case _ =>
        ZIO.succeed(None)
