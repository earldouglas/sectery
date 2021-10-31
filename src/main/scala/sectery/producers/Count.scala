package sectery.producers

import java.sql.Connection
import sectery.Db
import sectery.Info
import sectery.Producer
import sectery.Rx
import sectery.Tx
import zio.RIO
import zio.UIO
import zio.ZIO

object Count extends Producer:

  override def help(): Iterable[Info] =
    Some(Info("@count", "@count"))

  override def init(): RIO[Db.Db, Unit] =
    for
      _ <- Db { conn =>
        val s = "CREATE TABLE IF NOT EXISTS COUNTER(VALUE INT NOT NULL)"
        val stmt = conn.createStatement
        stmt.executeUpdate(s)
        stmt.close
      }
    yield ()

  override def apply(m: Rx): RIO[Db.Db, Iterable[Tx]] =
    m match
      case Rx(channel, _, "@count") =>
        def getOldCount(conn: Connection): Int =
          val q = "SELECT VALUE FROM COUNTER LIMIT 1"
          val stmt = conn.createStatement
          val rs = stmt.executeQuery(q)
          val count =
            if rs.next() then rs.getInt("VALUE")
            else 0
          stmt.close
          count

        def dropCount(conn: Connection): Unit =
          val s = "DELETE FROM COUNTER"
          val stmt = conn.createStatement
          stmt.executeUpdate(s)
          stmt.close

        def addCount(oldCount: Int)(conn: Connection): Int =
          val count = oldCount + 1
          val s = "INSERT INTO COUNTER (VALUE) VALUES (?)"
          val stmt = conn.prepareStatement(s)
          stmt.setInt(1, count)
          stmt.executeUpdate
          stmt.close
          count

        Db { conn =>
          val oldCount = getOldCount(conn)
          dropCount(conn)
          val newCount = addCount(oldCount)(conn)
          Some(Tx(channel, s"${newCount}"))
        }
      case _ =>
        ZIO.succeed(None)
