package sectery.adaptors

import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import sectery._
import sectery.effects._

object LiveTell:

  private def unsafeInit(c: Connection): Unit =
    val s =
      """|CREATE TABLE IF NOT EXISTS TELL (
         |  _CHANNEL_ VARCHAR(64) NOT NULL,
         |  _FROM_ VARCHAR(64) NOT NULL,
         |  _TO_ VARCHAR(64) NOT NULL,
         |  _MESSAGE_ VARCHAR(64) NOT NULL,
         |  _TIMESTAMP_ TIMESTAMP NOT NULL
         |)
         |""".stripMargin
    val stmt = c.createStatement
    stmt.executeUpdate(s)
    stmt.close

  private def unsafeSave(channel: String, m: Tell.Saved)(
      c: Connection
  ): Unit =
    val s =
      "INSERT INTO TELL (_CHANNEL_, _FROM_, _TO_, _MESSAGE_, _TIMESTAMP_) VALUES (?, ?, ?, ?, ?)"
    val stmt = c.prepareStatement(s)
    stmt.setString(1, channel)
    stmt.setString(2, m.from)
    stmt.setString(3, m.to)
    stmt.setString(4, m.message)
    stmt.setTimestamp(5, new Timestamp(m.date.toEpochMilli()))
    stmt.executeUpdate()
    stmt.close()

  private def unsafePop(channel: String, nick: String)(
      c: Connection
  ): List[Tell.Saved] =

    def findMessages(c: Connection): List[Tell.Saved] =
      val s =
        """|SELECT _FROM_, _MESSAGE_, _TIMESTAMP_
           |FROM TELL
           |WHERE _CHANNEL_ = ? AND _TO_ = ?
           |ORDER BY _TIMESTAMP_ DESC
           |""".stripMargin
      val stmt = c.prepareStatement(s)
      stmt.setString(1, channel)
      stmt.setString(2, nick)
      val rs = stmt.executeQuery()
      var msgs: List[Tell.Saved] = Nil
      while (rs.next()) {
        val from = rs.getString("_FROM_")
        val message = rs.getString("_MESSAGE_")
        val timestamp = rs.getTimestamp("_TIMESTAMP_")
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
        "DELETE FROM TELL WHERE _CHANNEL_ = ? AND _TO_ = ?"
      val stmt = c.prepareStatement(s)
      stmt.setString(1, channel)
      stmt.setString(2, nick)
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

      override def save(channel: String, m: Tell.Saved) =
        summon[Transactor[F]].transact { c =>
          unsafeSave(channel, m)(c)
        }

      override def pop(channel: String, nick: String) =
        summon[Transactor[F]].transact { c =>
          unsafePop(channel, nick)(c)
        }
