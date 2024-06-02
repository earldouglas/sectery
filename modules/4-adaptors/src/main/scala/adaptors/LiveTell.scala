package sectery.adaptors

import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import sectery._
import sectery.effects._

object LiveTell:

  private def unsafeInit(c: Connection): Unit =
    val s =
      """|CREATE TABLE IF NOT EXISTS `TELL_V2` (
         |  `SERVICE` VARCHAR(64) NOT NULL,
         |  `CHANNEL` VARCHAR(64) NOT NULL,
         |  `FROM` VARCHAR(64) NOT NULL,
         |  `TO` VARCHAR(64) NOT NULL,
         |  `MESSAGE` VARCHAR(64) NOT NULL,
         |  `TIMESTAMP` TIMESTAMP NOT NULL
         |)
         |""".stripMargin
    val stmt = c.createStatement
    stmt.executeUpdate(s)
    stmt.close

  private def unsafeSave(
      c: Connection,
      service: String,
      channel: String,
      m: Tell.Saved
  ): Unit =
    val s =
      """|INSERT INTO `TELL_V2` (`SERVICE`, `CHANNEL`, `FROM`, `TO`, `MESSAGE`, `TIMESTAMP`)
         |VALUES (?, ?, ?, ?, ?, ?)
         |""".stripMargin
    val stmt = c.prepareStatement(s)
    stmt.setString(1, service)
    stmt.setString(2, channel)
    stmt.setString(3, m.from)
    stmt.setString(4, m.to)
    stmt.setString(5, m.message)
    stmt.setTimestamp(6, new Timestamp(m.date.toEpochMilli()))
    stmt.executeUpdate()
    stmt.close()

  private def unsafePop(
      c: Connection,
      service: String,
      channel: String,
      nick: String
  ): List[Tell.Saved] =

    def findMessages(c: Connection): List[Tell.Saved] =
      val s =
        """|SELECT `FROM`, `MESSAGE`, `TIMESTAMP`
           |FROM `TELL_V2`
           |WHERE `SERVICE` = ?
           |  AND `CHANNEL` = ?
           |  AND `TO` = ?
           |ORDER BY `TIMESTAMP` DESC
           |""".stripMargin
      val stmt = c.prepareStatement(s)
      stmt.setString(1, service)
      stmt.setString(2, channel)
      stmt.setString(3, nick)
      val rs = stmt.executeQuery()
      var msgs: List[Tell.Saved] = Nil
      while (rs.next()) {
        val from = rs.getString("FROM")
        val message = rs.getString("MESSAGE")
        val timestamp = rs.getTimestamp("TIMESTAMP")
        msgs = Tell.Saved(
          to = nick,
          from = from,
          date = Instant.ofEpochSecond(timestamp.getTime()),
          message = message
        ) :: msgs
      }
      stmt.close
      msgs

    def dropMessages(c: Connection): Unit =
      val s =
        """|DELETE FROM `TELL_V2`
           |WHERE `SERVICE` = ?
           |  AND `CHANNEL` = ?
           |  AND `TO` = ?
           |""".stripMargin
      val stmt = c.prepareStatement(s)
      stmt.setString(1, service)
      stmt.setString(2, channel)
      stmt.setString(3, nick)
      stmt.executeUpdate()
      stmt.close

    val messages = findMessages(c)
    dropMessages(c)
    messages

  def apply[F[_]: Transactor](
      c: Connection
  ): Tell[F] =

    unsafeInit(c)

    new Tell[F]:

      override def save(
          service: String,
          channel: String,
          m: Tell.Saved
      ) =
        summon[Transactor[F]].transact { c =>
          unsafeSave(c, service, channel, m)
        }

      override def pop(service: String, channel: String, nick: String) =
        summon[Transactor[F]].transact { c =>
          unsafePop(c, service, channel, nick)
        }
