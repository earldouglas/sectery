package sectery.producers

import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import sectery.Db
import sectery.Info
import sectery.Producer
import sectery.Rx
import sectery.Tx
import zio.Clock
import zio.Has
import zio.RIO
import zio.UIO
import zio.ZIO

object Grab extends Producer:

  private val grab = """^@grab\s+([^\s]+)\s*$""".r
  private val quoteNick = """^@quote\s+([^\s]+)\s*$""".r

  override def help(): Iterable[Info] =
    List(
      Info("@grab", "@grab <nick>"),
      Info("@quote", "@quote [nick]")
    )

  override def init(): RIO[Db.Db, Unit] =
    for
      _ <- Db.query { conn =>
        val s =
          """|CREATE TABLE IF NOT EXISTS
             |GRABBED_MESSAGES(
             |  CHANNEL VARCHAR(256) NOT NULL,
             |  NICK VARCHAR(256) NOT NULL,
             |  MESSAGE TEXT NOT NULL,
             |  TIMESTAMP TIMESTAMP NOT NULL
             |)
             |""".stripMargin
        val stmt = conn.createStatement
        stmt.executeUpdate(s)
        stmt.close
      }
    yield ()

  override def apply(m: Rx): RIO[Db.Db with Has[Clock], Iterable[Tx]] =
    m match
      case Rx(c, from, grab(nick)) if from == nick =>
        ZIO.succeed(
          Some(Tx(c, "You can't grab yourself."))
        )
      case Rx(c, _, grab(nick)) =>
        for
          now <- Clock.currentTime(TimeUnit.MILLISECONDS)
          mo <- LastMessage.lastMessage(c, nick)
          reply <-
            mo match
              case Some(m) =>
                Db.query { conn =>
                  val s =
                    """|INSERT INTO GRABBED_MESSAGES (
                       |  CHANNEL, NICK, MESSAGE, TIMESTAMP
                       |) VALUES (?, ?, ?, ?)
                       |""".stripMargin
                  val stmt = conn.prepareStatement(s)
                  stmt.setString(1, c)
                  stmt.setString(2, nick)
                  stmt.setString(3, m.message)
                  stmt.setTimestamp(4, new Timestamp(now))
                  stmt.executeUpdate()
                  stmt.close
                  Some(Tx(c, s"Grabbed ${nick}."))
                }
              case None =>
                ZIO.succeed(
                  Some(Tx(c, s"${nick} hasn't said anything."))
                )
        yield reply
      case Rx(c, _, quoteNick(nick)) =>
        randomGrabbedMessage(c, nick) map {
          case Some(m) =>
            Some(Tx(c, s"<${nick}> ${m}"))
          case None =>
            Some(Tx(c, s"${nick} hasn't said anything."))
        }
      case Rx(c, _, "@quote") =>
        randomGrabbedMessage(c) map {
          case Some(gm) =>
            Some(Tx(c, s"<${gm.nick}> ${gm.message}"))
          case None =>
            Some(Tx(c, s"Nobody has said anything."))
        }
      case _ =>
        ZIO.succeed(None)

  case class GrabbedMessage(nick: String, message: String)

  private def randomGrabbedNick(
      channel: String
  ): RIO[Db.Db, Option[String]] =
    Db.query { conn =>
      val s =
        """|SELECT NICK
           |FROM (
           |  SELECT DISTINCT(NICK)
           |  FROM GRABBED_MESSAGES
           |  WHERE CHANNEL = ?
           |) NICKS
           |ORDER BY RANDOM()
           |LIMIT 1
           |""".stripMargin
      val stmt = conn.prepareStatement(s)
      stmt.setString(1, channel)
      val rs = stmt.executeQuery
      var no: Option[String] = None
      if (rs.next()) {
        val nick = rs.getString("NICK")
        no = Some(nick)
      }
      stmt.close
      no
    }

  private def randomGrabbedMessage(
      channel: String,
      nick: String
  ): RIO[Db.Db, Option[String]] =
    Db.query { conn =>
      val s =
        """|SELECT MESSAGE
           |FROM GRABBED_MESSAGES
           |WHERE CHANNEL = ?
           |  AND NICK = ?
           |ORDER BY RANDOM()
           |LIMIT 1
           |""".stripMargin
      val stmt = conn.prepareStatement(s)
      stmt.setString(1, channel)
      stmt.setString(2, nick)
      val rs = stmt.executeQuery
      var mo: Option[String] = None
      if (rs.next()) {
        val message = rs.getString("MESSAGE")
        mo = Some(message)
      }
      stmt.close
      mo
    }

  def randomGrabbedMessage(
      channel: String
  ): RIO[Db.Db, Option[GrabbedMessage]] =
    for
      no <- randomGrabbedNick(channel)
      gmo <- no match {
        case Some(nick) =>
          randomGrabbedMessage(channel = channel, nick = nick).map {
            mo =>
              mo.map { message =>
                GrabbedMessage(nick = nick, message = message)
              }
          }
        case None => ZIO.succeed(None)
      }
    yield gmo
