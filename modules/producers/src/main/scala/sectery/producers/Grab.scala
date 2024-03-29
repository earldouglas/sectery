package sectery.producers

import java.sql.Connection
import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import sectery.Db
import sectery.Producer
import sectery.Rx
import sectery.Tx
import zio.Clock
import zio.ZIO

object Grab extends Producer:

  private val grab = """^@grab\s+([^\s]+)\s*$""".r
  private val quoteNick = """^@quote\s+([^\s]+)\s*$""".r

  override def help(): Iterable[Info] =
    List(
      Info("@grab", "@grab <nick>"),
      Info("@quote", "@quote [nick]")
    )

  override def init(): ZIO[Db.Db, Throwable, Unit] =
    for _ <- Db { conn =>
        val s =
          """|CREATE TABLE IF NOT EXISTS
             |`GRABBED_MESSAGES`(
             |  `CHANNEL` VARCHAR(256) NOT NULL,
             |  `NICK` VARCHAR(256) NOT NULL,
             |  `MESSAGE` TEXT NOT NULL,
             |  `TIMESTAMP` TIMESTAMP NOT NULL
             |)
             |""".stripMargin
        val stmt = conn.createStatement
        stmt.executeUpdate(s)
        stmt.close
      }
    yield ()

  override def apply(
      m: Rx
  ): ZIO[Db.Db with Clock, Throwable, Iterable[Tx]] =
    m match
      case Rx(c, from, grab(nick)) if from == nick =>
        ZIO.succeed(
          Some(Tx(c, "You can't grab yourself."))
        )
      case Rx(c, _, grab(nick)) =>
        for
          now <- Clock.currentTime(TimeUnit.MILLISECONDS)
          reply <-
            Db { conn =>
              LastMessage.lastMessage(c, nick)(conn) match
                case Some(m) =>
                  val s =
                    """|INSERT INTO `GRABBED_MESSAGES` (
                       |  `CHANNEL`, `NICK`, `MESSAGE`, `TIMESTAMP`
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
                case None =>
                  Some(Tx(c, s"${nick} hasn't said anything."))
            }
        yield reply
      case Rx(c, _, quoteNick(nick)) =>
        Db { conn =>
          randomGrabbedMessage(c, nick)(conn) match {
            case Some(gm) =>
              Some(Tx(c, s"<${gm.nick}> ${gm.message}"))
            case None =>
              Some(Tx(c, s"${nick} hasn't said anything."))
          }
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

  private def randomGrabbedMessage(channel: String)(
      conn: Connection
  ): Option[GrabbedMessage] =
    val s =
      """|SELECT `NICK`, `MESSAGE`
         |FROM `GRABBED_MESSAGES`
         |WHERE `CHANNEL` = ?
         |ORDER BY RAND()
         |LIMIT 1
         |""".stripMargin
    val stmt = conn.prepareStatement(s)
    stmt.setString(1, channel)
    val rs = stmt.executeQuery
    var gmo: Option[GrabbedMessage] = None
    if rs.next() then
      val nick = rs.getString("NICK")
      val message = rs.getString("MESSAGE")
      gmo = Some(GrabbedMessage(nick = nick, message = message))
    stmt.close
    gmo

  private def randomGrabbedMessage(channel: String, nick: String)(
      conn: Connection
  ): Option[GrabbedMessage] =
    val s =
      """|SELECT `NICK`, `MESSAGE`
         |FROM `GRABBED_MESSAGES`
         |WHERE `CHANNEL` = ?
         |  AND `NICK` = ?
         |ORDER BY RAND()
         |LIMIT 1
         |""".stripMargin
    val stmt = conn.prepareStatement(s)
    stmt.setString(1, channel)
    stmt.setString(2, nick)
    val rs = stmt.executeQuery
    var gmo: Option[GrabbedMessage] = None
    if rs.next() then
      val nick = rs.getString("NICK")
      val message = rs.getString("MESSAGE")
      gmo = Some(GrabbedMessage(nick = nick, message = message))
    stmt.close
    gmo

  def randomGrabbedMessage(
      channel: String
  ): ZIO[Db.Db, Throwable, Option[GrabbedMessage]] =
    Db { conn =>
      randomGrabbedMessage(channel)(conn)
    }
