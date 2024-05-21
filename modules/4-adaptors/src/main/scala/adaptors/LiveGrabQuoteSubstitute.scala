package sectery.adaptors

import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import sectery._
import sectery.control.Monad
import sectery.control.Monad._
import sectery.domain.entities.Rx
import sectery.domain.entities.Tx
import sectery.effects.LastMessage.LastRx
import sectery.effects.Quote.GrabbedMessage
import sectery.effects._

object LiveGrabQuoteSubstitute:

  private def unsafeInit(c: Connection): Unit =
    val s1 =
      """|CREATE TABLE IF NOT EXISTS
         |`GRABBED_MESSAGES`(
         |  `CHANNEL` VARCHAR(256) NOT NULL,
         |  `NICK` VARCHAR(256) NOT NULL,
         |  `MESSAGE` TEXT NOT NULL,
         |  `TIMESTAMP` TIMESTAMP NOT NULL
         |)
         |""".stripMargin
    val stmt1 = c.createStatement
    stmt1.executeUpdate(s1)
    stmt1.close

    val s2 =
      """|CREATE TABLE IF NOT EXISTS
         |`LAST_MESSAGE`(
         |  `CHANNEL` VARCHAR(256) NOT NULL,
         |  `NICK` VARCHAR(256) NOT NULL,
         |  `MESSAGE` TEXT NOT NULL,
         |  `TIMESTAMP` TIMESTAMP NOT NULL,
         |  PRIMARY KEY (`CHANNEL`, `NICK`)
         |)
         |""".stripMargin
    val stmt2 = c.createStatement
    stmt2.executeUpdate(s2)
    stmt2.close()

    val s3 =
      """|CREATE TABLE IF NOT EXISTS
         |`AUTOQUOTE`(
         |  `CHANNEL` VARCHAR(256) NOT NULL,
         |  `TIMESTAMP` TIMESTAMP NOT NULL,
         |  PRIMARY KEY (`CHANNEL`)
         |)
         |""".stripMargin
    val stmt3 = c.createStatement
    stmt3.executeUpdate(s3)
    stmt3.close()

  private def unsafeLastRx(channel: String)(
      c: Connection
  ): Option[LastRx] =
    val s =
      """|SELECT `NICK`, `MESSAGE`, `TIMESTAMP`
         |FROM `LAST_MESSAGE`
         |WHERE `CHANNEL` = ?
         |ORDER BY `TIMESTAMP` DESC
         |LIMIT 1
         |""".stripMargin
    val stmt = c.prepareStatement(s)
    stmt.setString(1, channel)
    val rs = stmt.executeQuery
    var mo: Option[LastRx] = None
    if rs.next() then
      val nick = rs.getString("NICK")
      val message = rs.getString("MESSAGE")
      val timestamp = rs.getTimestamp("TIMESTAMP")
      mo = Some(
        LastRx(
          channel = channel,
          nick = nick,
          message = message,
          instant = Instant.ofEpochMilli(timestamp.getTime())
        )
      )
    stmt.close()
    mo

  private def unsafeGrab(channel: String)(
      c: Connection
  ): Option[String] =
    unsafeLastRx(channel)(c) match
      case Some(m) =>
        val s =
          """|INSERT INTO `GRABBED_MESSAGES` (
             |  `CHANNEL`, `NICK`, `MESSAGE`, `TIMESTAMP`
             |) VALUES (?, ?, ?, ?)
             |""".stripMargin
        val stmt = c.prepareStatement(s)
        stmt.setString(1, channel)
        stmt.setString(2, m.nick)
        stmt.setString(3, m.message)
        stmt.setTimestamp(4, new Timestamp(m.instant.toEpochMilli()))
        stmt.executeUpdate()
        stmt.close
        Some(m.nick)
      case None =>
        None

  private def unsafeLastRx(channel: String, nick: String)(
      c: Connection
  ): Option[LastRx] =
    val s =
      """|SELECT `MESSAGE`, `TIMESTAMP`
         |FROM `LAST_MESSAGE`
         |WHERE `CHANNEL` = ?
         |  AND `NICK` = ?
         |ORDER BY `TIMESTAMP` DESC
         |LIMIT 1
         |""".stripMargin
    val stmt = c.prepareStatement(s)
    stmt.setString(1, channel)
    stmt.setString(2, nick)
    val rs = stmt.executeQuery
    var mo: Option[LastRx] = None
    if rs.next() then
      val message = rs.getString("MESSAGE")
      val timestamp = rs.getTimestamp("TIMESTAMP")
      mo = Some(
        LastRx(
          channel = channel,
          nick = nick,
          message = message,
          instant = timestamp.toInstant
        )
      )
    stmt.close()
    mo

  private def unsafeGrab(channel: String, nick: String)(
      c: Connection
  ): Boolean =
    unsafeLastRx(channel, nick)(c) match
      case Some(m) =>
        val s =
          """|INSERT INTO `GRABBED_MESSAGES` (
             |  `CHANNEL`, `NICK`, `MESSAGE`, `TIMESTAMP`
             |) VALUES (?, ?, ?, ?)
             |""".stripMargin
        val stmt = c.prepareStatement(s)
        stmt.setString(1, channel)
        stmt.setString(2, nick)
        stmt.setString(3, m.message)
        stmt.setTimestamp(4, new Timestamp(m.instant.toEpochMilli()))
        stmt.executeUpdate()
        stmt.close
        true
      case None =>
        false

  private def unsafeRandomGrabbedMessage(channel: String)(
      c: Connection
  ): Option[GrabbedMessage] =
    val s =
      """|SELECT `CHANNEL`, `NICK`, `MESSAGE`, `TIMESTAMP`
         |FROM `GRABBED_MESSAGES`
         |WHERE `CHANNEL` = ?
         |ORDER BY RAND()
         |LIMIT 1
         |""".stripMargin
    val stmt = c.prepareStatement(s)
    stmt.setString(1, channel)
    val rs = stmt.executeQuery
    var gmo: Option[GrabbedMessage] = None
    if rs.next() then
      val channel = rs.getString("CHANNEL")
      val nick = rs.getString("NICK")
      val message = rs.getString("MESSAGE")
      val timestamp = rs.getTimestamp("TIMESTAMP")
      gmo = Some(
        GrabbedMessage(
          channel = channel,
          nick = nick,
          message = message,
          instant = timestamp.toInstant
        )
      )
    stmt.close
    gmo

  private def unsafeRandomGrabbedMessage(channel: String, nick: String)(
      c: Connection
  ): Option[GrabbedMessage] =
    val s =
      """|SELECT `CHANNEL`, `NICK`, `MESSAGE`, `TIMESTAMP`
         |FROM `GRABBED_MESSAGES`
         |WHERE `CHANNEL` = ?
         |  AND `NICK` = ?
         |ORDER BY RAND()
         |LIMIT 1
         |""".stripMargin
    val stmt = c.prepareStatement(s)
    stmt.setString(1, channel)
    stmt.setString(2, nick)
    val rs = stmt.executeQuery
    var gmo: Option[GrabbedMessage] = None
    if rs.next() then
      val channel = rs.getString("CHANNEL")
      val nick = rs.getString("NICK")
      val message = rs.getString("MESSAGE")
      val timestamp = rs.getTimestamp("TIMESTAMP")
      gmo = Some(
        GrabbedMessage(
          channel = channel,
          nick = nick,
          message = message,
          instant = timestamp.toInstant
        )
      )
    stmt.close
    gmo

  def unsafeSaveMessage(rx: Rx, nowMillis: Long)(c: Connection): Unit =

    def deleteLastRx(c: Connection): Unit =
      val s =
        "DELETE FROM `LAST_MESSAGE` WHERE `CHANNEL` = ? AND `NICK` = ?"
      val stmt = c.prepareStatement(s)
      stmt.setString(1, rx.channel)
      stmt.setString(2, rx.nick)
      stmt.executeUpdate
      stmt.close()

    def addLastRx(nowMillis: Long)(c: Connection): Unit =
      val s =
        "INSERT INTO `LAST_MESSAGE` (`CHANNEL`, `NICK`, `MESSAGE`, `TIMESTAMP`) VALUES (?, ?, ?, ?)"
      val stmt = c.prepareStatement(s)
      stmt.setString(1, rx.channel)
      stmt.setString(2, rx.nick)
      stmt.setString(3, rx.message)
      stmt.setTimestamp(4, new Timestamp(nowMillis))
      stmt.executeUpdate
      stmt.close()

    def deleteAutoquote(c: Connection): Unit =
      val s =
        "DELETE FROM `AUTOQUOTE` WHERE `CHANNEL` = ?"
      val stmt = c.prepareStatement(s)
      stmt.setString(1, rx.channel)
      stmt.executeUpdate
      stmt.close()

    def addAutoquote(nowMillis: Long)(c: Connection): Unit =
      val s =
        "INSERT INTO `AUTOQUOTE` (`CHANNEL`, `TIMESTAMP`) VALUES (?, ?)"
      val stmt = c.prepareStatement(s)
      stmt.setString(1, rx.channel)
      stmt.setTimestamp(2, new Timestamp(nowMillis))
      stmt.executeUpdate
      stmt.close()

    deleteLastRx(c)
    addLastRx(nowMillis)(c)
    deleteAutoquote(c)
    addAutoquote(nowMillis)(c)

  private def unsafeGetAutoquoteChannels(
      nowMillis: Long
  )(c: Connection): List[String] =

    def getChannels(c: Connection): List[String] =
      val s =
        """|SELECT CHANNEL
           |FROM AUTOQUOTE
           |WHERE TIMESTAMP <= ?
           |""".stripMargin
      val stmt = c.prepareStatement(s)
      val fourtyFiveMinutesAgoMillis = nowMillis - 45L * 60L * 1000L
      stmt.setTimestamp(1, new Timestamp(fourtyFiveMinutesAgoMillis))
      val rs = stmt.executeQuery
      var cs: List[String] = Nil
      if rs.next() then
        val channel = rs.getString("CHANNEL")
        cs = channel :: cs
      stmt.close()
      cs

    def updateAutoquote(
        channels: List[String]
    )(c: Connection): Unit =
      if channels.length > 0 then
        val in = channels.map(_ => "?").mkString(", ")
        val s =
          s"""|UPDATE AUTOQUOTE
              |SET TIMESTAMP = ?
              |WHERE CHANNEL IN (${in})
              |""".stripMargin
        val stmt = c.prepareStatement(s)
        stmt.setTimestamp(1, new Timestamp(nowMillis))
        channels.zipWithIndex
          .foreach { case (channel, index) =>
            stmt.setString(2 + index, channel)
          }
        stmt.execute()
        stmt.close()
      else ()

    val channels = getChannels(c)
    updateAutoquote(channels)(c)
    channels

  private def unsafeGetAutoquoteMessages(
      channels: List[String]
  )(c: Connection): List[Tx] =
    channels.flatMap { channel =>
      unsafeRandomGrabbedMessage(channel)(c).map { gm =>
        Tx(
          channel = channel,
          message = s"<${gm.nick}> ${gm.message}"
        )
      }
    }

  def apply[F[_]: Transactor: Now: Monad](
      c: Connection
  ): Grab[F] & Quote[F] & Autoquote[F] & LastMessage[F] =

    unsafeInit(c)

    new Grab[F] with Quote[F] with Autoquote[F] with LastMessage[F]:

      override def grab(channel: String, nick: String): F[Boolean] =
        summon[Transactor[F]].transact { c =>
          unsafeGrab(channel, nick)(c)
        }

      override def grab(channel: String): F[Option[String]] =
        summon[Transactor[F]].transact { c =>
          unsafeGrab(channel)(c)
        }

      override def quote(
          channel: String,
          nick: String
      ): F[Option[GrabbedMessage]] =
        summon[Transactor[F]].transact { c =>
          unsafeRandomGrabbedMessage(channel, nick)(c)
        }

      override def quote(channel: String): F[Option[GrabbedMessage]] =
        summon[Transactor[F]].transact { c =>
          unsafeRandomGrabbedMessage(channel)(c)
        }

      override def getAutoquoteMessages(): F[List[Tx]] =
        summon[Now[F]]
          .now()
          .map(_.toEpochMilli())
          .map { nowMillis =>
            val channels = unsafeGetAutoquoteChannels(nowMillis)(c)
            val messages = unsafeGetAutoquoteMessages(channels)(c)
            messages
          }

      override def saveLastMessage(rx: Rx): F[Unit] =
        summon[Now[F]]
          .now()
          .map(_.toEpochMilli())
          .flatMap { nowMillis =>
            summon[Transactor[F]].transact { c =>
              unsafeSaveMessage(rx, nowMillis)(c)
            }
          }

      override def getLastMessages(channel: String): F[List[LastRx]] =
        summon[Transactor[F]].transact { c =>
          unsafeLastRx(channel)(c).toList
        }
