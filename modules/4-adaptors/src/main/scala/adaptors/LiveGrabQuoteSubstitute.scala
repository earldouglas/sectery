package sectery.adaptors

import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import sectery._
import sectery.control.Monad
import sectery.control.Monad._
import sectery.domain.entities._
import sectery.effects.LastMessage.LastRx
import sectery.effects.Quote.GrabbedMessage
import sectery.effects._

object LiveGrabQuoteSubstitute:

  private def executeUpdate(c: Connection, sql: String): Unit =
    val stmt = c.createStatement
    stmt.executeUpdate(sql)
    stmt.close

  private def unsafeInit(c: Connection): Unit =
    executeUpdate(
      c,
      """|CREATE TABLE IF NOT EXISTS
         |`GRABBED_MESSAGES_V2`(
         |  `SERVICE` VARCHAR(256) NOT NULL,
         |  `CHANNEL` VARCHAR(256) NOT NULL,
         |  `NICK` VARCHAR(256) NOT NULL,
         |  `MESSAGE` TEXT NOT NULL,
         |  `TIMESTAMP` TIMESTAMP NOT NULL
         |)
         |""".stripMargin
    )

    executeUpdate(
      c,
      """|CREATE TABLE IF NOT EXISTS
         |`LAST_MESSAGE_V2`(
         |  `SERVICE` VARCHAR(256) NOT NULL,
         |  `CHANNEL` VARCHAR(256) NOT NULL,
         |  `NICK` VARCHAR(256) NOT NULL,
         |  `MESSAGE` TEXT NOT NULL,
         |  `TIMESTAMP` TIMESTAMP NOT NULL,
         |  PRIMARY KEY (`SERVICE`, `CHANNEL`, `NICK`)
         |)
         |""".stripMargin
    )

    executeUpdate(
      c,
      """|CREATE TABLE IF NOT EXISTS
         |`AUTOQUOTE_V2`(
         |  `SERVICE` VARCHAR(256) NOT NULL,
         |  `CHANNEL` VARCHAR(256) NOT NULL,
         |  `TIMESTAMP` TIMESTAMP NOT NULL,
         |  PRIMARY KEY (`SERVICE`, `CHANNEL`)
         |)
         |""".stripMargin
    )

  private def unsafeLastRx(
      c: Connection,
      service: String,
      channel: String
  ): Option[LastRx] =
    val s =
      """|SELECT `NICK`, `MESSAGE`, `TIMESTAMP`
         |FROM `LAST_MESSAGE_V2`
         |WHERE `SERVICE` = ?
         |  AND `CHANNEL` = ?
         |ORDER BY `TIMESTAMP` DESC
         |LIMIT 1
         |""".stripMargin
    val stmt = c.prepareStatement(s)
    stmt.setString(1, service)
    stmt.setString(2, channel)
    val rs = stmt.executeQuery
    var mo: Option[LastRx] = None
    if rs.next() then
      val nick = rs.getString("NICK")
      val message = rs.getString("MESSAGE")
      val timestamp = rs.getTimestamp("TIMESTAMP")
      mo = Some(
        LastRx(
          service = service,
          channel = channel,
          nick = nick,
          message = message,
          instant = Instant.ofEpochMilli(timestamp.getTime())
        )
      )
    stmt.close()
    mo

  private def unsafeGrab(
      c: Connection,
      service: String,
      channel: String
  ): Option[String] =
    unsafeLastRx(c, service, channel) match
      case Some(m) =>
        val s =
          """|INSERT INTO `GRABBED_MESSAGES_V2` (
             |  `SERVICE`, `CHANNEL`, `NICK`, `MESSAGE`, `TIMESTAMP`
             |) VALUES (?, ?, ?, ?, ?)
             |""".stripMargin
        val stmt = c.prepareStatement(s)
        stmt.setString(1, service)
        stmt.setString(2, channel)
        stmt.setString(3, m.nick)
        stmt.setString(4, m.message)
        stmt.setTimestamp(5, new Timestamp(m.instant.toEpochMilli()))
        stmt.executeUpdate()
        stmt.close
        Some(m.nick)
      case None =>
        None

  private def unsafeLastRx(
      c: Connection,
      service: String,
      channel: String,
      nick: String
  ): Option[LastRx] =
    val s =
      """|SELECT `MESSAGE`, `TIMESTAMP`
         |FROM `LAST_MESSAGE_V2`
         |WHERE `SERVICE` = ?
         |  AND `CHANNEL` = ?
         |  AND `NICK` = ?
         |ORDER BY `TIMESTAMP` DESC
         |LIMIT 1
         |""".stripMargin
    val stmt = c.prepareStatement(s)
    stmt.setString(1, service)
    stmt.setString(2, channel)
    stmt.setString(3, nick)
    val rs = stmt.executeQuery
    var mo: Option[LastRx] = None
    if rs.next() then
      val message = rs.getString("MESSAGE")
      val timestamp = rs.getTimestamp("TIMESTAMP")
      mo = Some(
        LastRx(
          service = service,
          channel = channel,
          nick = nick,
          message = message,
          instant = timestamp.toInstant
        )
      )
    stmt.close()
    mo

  private def unsafeGrab(
      c: Connection,
      service: String,
      channel: String,
      nick: String
  ): Boolean =
    unsafeLastRx(c, service, channel, nick) match
      case Some(m) =>
        val s =
          """|INSERT INTO `GRABBED_MESSAGES_V2` (
             |  `SERVICE`, `CHANNEL`, `NICK`, `MESSAGE`, `TIMESTAMP`
             |) VALUES (?, ?, ?, ?, ?)
             |""".stripMargin
        val stmt = c.prepareStatement(s)
        stmt.setString(1, service)
        stmt.setString(2, channel)
        stmt.setString(3, nick)
        stmt.setString(4, m.message)
        stmt.setTimestamp(5, new Timestamp(m.instant.toEpochMilli()))
        stmt.executeUpdate()
        stmt.close
        true
      case None =>
        false

  private def unsafeRandomGrabbedMessage(
      c: Connection,
      service: String,
      channel: String
  ): Option[GrabbedMessage] =
    val s =
      """|SELECT `NICK`, `MESSAGE`, `TIMESTAMP`
         |FROM `GRABBED_MESSAGES_V2`
         |WHERE `SERVICE` = ?
         |  AND `CHANNEL` = ?
         |ORDER BY RAND()
         |LIMIT 1
         |""".stripMargin
    val stmt = c.prepareStatement(s)
    stmt.setString(1, service)
    stmt.setString(2, channel)
    val rs = stmt.executeQuery
    var gmo: Option[GrabbedMessage] = None
    if rs.next() then
      val nick = rs.getString("NICK")
      val message = rs.getString("MESSAGE")
      val timestamp = rs.getTimestamp("TIMESTAMP")
      gmo = Some(
        GrabbedMessage(
          service = service,
          channel = channel,
          nick = nick,
          message = message,
          instant = timestamp.toInstant
        )
      )
    stmt.close
    gmo

  private def unsafeRandomGrabbedMessage(
      c: Connection,
      service: String,
      channel: String,
      nick: String
  ): Option[GrabbedMessage] =
    val s =
      """|SELECT `NICK`, `MESSAGE`, `TIMESTAMP`
         |FROM `GRABBED_MESSAGES_V2`
         |WHERE `SERVICE` = ?
         |  AND `CHANNEL` = ?
         |  AND `NICK` = ?
         |ORDER BY RAND()
         |LIMIT 1
         |""".stripMargin
    val stmt = c.prepareStatement(s)
    stmt.setString(1, service)
    stmt.setString(2, channel)
    stmt.setString(3, nick)
    val rs = stmt.executeQuery
    var gmo: Option[GrabbedMessage] = None
    if rs.next() then
      val nick = rs.getString("NICK")
      val message = rs.getString("MESSAGE")
      val timestamp = rs.getTimestamp("TIMESTAMP")
      gmo = Some(
        GrabbedMessage(
          service = service,
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
        """|DELETE FROM `LAST_MESSAGE_V2`
           |WHERE `SERVICE` = ?
           |  AND `CHANNEL` = ?
           |  AND `NICK` = ?
           |""".stripMargin
      val stmt = c.prepareStatement(s)
      stmt.setString(1, rx.service)
      stmt.setString(2, rx.channel)
      stmt.setString(3, rx.nick)
      stmt.executeUpdate
      stmt.close()

    def addLastRx(nowMillis: Long)(c: Connection): Unit =
      val s =
        """|INSERT INTO `LAST_MESSAGE_V2` (`SERVICE`, `CHANNEL`, `NICK`, `MESSAGE`, `TIMESTAMP`)
           |VALUES (?, ?, ?, ?, ?)
           |""".stripMargin
      val stmt = c.prepareStatement(s)
      stmt.setString(1, rx.service)
      stmt.setString(2, rx.channel)
      stmt.setString(3, rx.nick)
      stmt.setString(4, rx.message)
      stmt.setTimestamp(5, new Timestamp(nowMillis))
      stmt.executeUpdate
      stmt.close()

    def deleteAutoquote(c: Connection): Unit =
      val s =
        """|DELETE FROM `AUTOQUOTE_V2`
           |WHERE `SERVICE` = ?
           |  AND `CHANNEL` = ?
           |""".stripMargin
      val stmt = c.prepareStatement(s)
      stmt.setString(1, rx.service)
      stmt.setString(2, rx.channel)
      stmt.executeUpdate
      stmt.close()

    def addAutoquote(nowMillis: Long)(c: Connection): Unit =
      val s =
        """|INSERT INTO `AUTOQUOTE_V2` (`SERVICE`, `CHANNEL`, `TIMESTAMP`)
           |VALUES (?, ?, ?)
           |""".stripMargin
      val stmt = c.prepareStatement(s)
      stmt.setString(1, rx.service)
      stmt.setString(2, rx.channel)
      stmt.setTimestamp(3, new Timestamp(nowMillis))
      stmt.executeUpdate
      stmt.close()

    deleteLastRx(c)
    addLastRx(nowMillis)(c)
    deleteAutoquote(c)
    addAutoquote(nowMillis)(c)

  private def unsafeGetAutoquoteChannels(
      nowMillis: Long
  )(c: Connection): List[(String, String)] =

    def getServiceChannels(c: Connection): List[(String, String)] =
      val s =
        """|SELECT SERVICE, CHANNEL
           |FROM AUTOQUOTE_V2
           |WHERE TIMESTAMP <= ?
           |""".stripMargin
      val stmt = c.prepareStatement(s)
      val fourtyFiveMinutesAgoMillis = nowMillis - 45L * 60L * 1000L
      stmt.setTimestamp(1, new Timestamp(fourtyFiveMinutesAgoMillis))
      val rs = stmt.executeQuery
      var scs: List[(String, String)] = Nil
      if rs.next() then
        val service = rs.getString("SERVICE")
        val channel = rs.getString("CHANNEL")
        scs = (service, channel) :: scs
      stmt.close()
      scs

    def updateAutoquote(
        scs: List[(String, String)]
    )(c: Connection): Unit =
      if scs.length > 0 then
        val now = new Timestamp(nowMillis)
        val s =
          s"""|UPDATE AUTOQUOTE_V2
              |SET TIMESTAMP = ?
              |WHERE SERVICE = ?
              |  AND CHANNEL = ?
              |""".stripMargin
        scs
          .foreach { case (service, channel) =>
            val stmt = c.prepareStatement(s)
            stmt.setTimestamp(1, now)
            stmt.setString(2, service)
            stmt.setString(3, channel)
            stmt.execute()
            stmt.close()
          }
      else ()

    val scs = getServiceChannels(c)
    updateAutoquote(scs)(c)
    scs

  private def unsafeGetAutoquoteMessages(
      scs: List[(String, String)]
  )(c: Connection): List[Tx] =
    scs.flatMap { (service, channel) =>
      unsafeRandomGrabbedMessage(c, service, channel)
        .map { gm =>
          Tx(
            service = service,
            channel = channel,
            thread = None,
            message = s"<${gm.nick}> ${gm.message}"
          )
        }
    }

  def apply[F[_]: Transactor: Now: Monad](
      c: Connection
  ): Grab[F] & Quote[F] & Autoquote[F] & LastMessage[F] =

    unsafeInit(c)

    new Grab[F] with Quote[F] with Autoquote[F] with LastMessage[F]:

      override def grab(
          service: String,
          channel: String,
          nick: String
      ): F[Boolean] =
        summon[Transactor[F]].transact { c =>
          unsafeGrab(c, service, channel, nick)
        }

      override def grab(
          service: String,
          channel: String
      ): F[Option[String]] =
        summon[Transactor[F]].transact { c =>
          unsafeGrab(c, service, channel)
        }

      override def quote(
          service: String,
          channel: String,
          nick: String
      ): F[Option[GrabbedMessage]] =
        summon[Transactor[F]].transact { c =>
          unsafeRandomGrabbedMessage(c, service, channel, nick)
        }

      override def quote(
          service: String,
          channel: String
      ): F[Option[GrabbedMessage]] =
        summon[Transactor[F]].transact { c =>
          unsafeRandomGrabbedMessage(c, service, channel)
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

      override def getLastMessages(
          service: String,
          channel: String
      ): F[List[LastRx]] =
        summon[Transactor[F]].transact { c =>
          unsafeLastRx(c, service, channel).toList
        }
