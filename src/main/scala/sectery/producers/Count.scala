package sectery.producers

import org.slf4j.LoggerFactory
import sectery.Db
import sectery.Info
import sectery.Producer
import sectery.Rx
import sectery.Tx
import zio.RIO
import zio.UIO
import zio.URIO
import zio.ZIO

object Count extends Producer:

  override def help(): Iterable[Info] =
    Some(Info("@count", "@count"))

  override def init(): RIO[Db.Db, Unit] =
    for
      _ <- Db.query { conn =>
        val s = "CREATE TABLE IF NOT EXISTS COUNTER(VALUE INT NOT NULL)"
        val stmt = conn.createStatement
        stmt.executeUpdate(s)
        stmt.close
      }
    yield ()

  override def apply(m: Rx): URIO[Db.Db, Iterable[Tx]] =
    m match
      case Rx(channel, _, "@count") =>
        val increment =
          for
            oldCount <- Db.query { conn =>
              val q = "SELECT VALUE FROM COUNTER LIMIT 1"
              val stmt = conn.createStatement
              val rs = stmt.executeQuery(q)
              val count =
                if (rs.next()) {
                  rs.getInt("VALUE")
                } else {
                  0
                }
              stmt.close
              count
            }
            _ <- Db.query { conn =>
              val s = "DELETE FROM COUNTER"
              val stmt = conn.createStatement
              stmt.executeUpdate(s)
              stmt.close
            }
            newCount <- Db.query { conn =>
              val count = oldCount + 1
              val s = "INSERT INTO COUNTER (VALUE) VALUES (?)"
              val stmt = conn.prepareStatement(s)
              stmt.setInt(1, count)
              stmt.executeUpdate
              stmt.close
              count
            }
          yield Some(Tx(channel, s"${newCount}"))
        increment.catchAll { e =>
          LoggerFactory.getLogger(this.getClass()).error("caught exception", e)
          ZIO.effectTotal(None)
        }.map(_.toIterable)
      case _ =>
        ZIO.effectTotal(None)
