package sectery.adaptors

import java.sql.Connection
import sectery.effects.Counter
import sectery.effects.Transactor

object LiveCounter:

  private def unsafeInit(c: Connection): Unit =
    val s =
      "CREATE TABLE IF NOT EXISTS `COUNTER` (`VALUE` INT NOT NULL)"
    val stmt = c.createStatement
    stmt.executeUpdate(s)
    stmt.close

  private def unsafeIncrementAndGet(c: Connection): Int =
    def getOldCount(c: Connection): Int =
      val q = "SELECT `VALUE` FROM `COUNTER` LIMIT 1"
      val stmt = c.createStatement
      val rs = stmt.executeQuery(q)
      val count =
        if rs.next() then rs.getInt("VALUE")
        else 0
      stmt.close
      count

    def dropCount(c: Connection): Unit =
      val s = "DELETE FROM COUNTER"
      val stmt = c.createStatement
      stmt.executeUpdate(s)
      stmt.close

    def addCount(oldCount: Int)(c: Connection): Int =
      val count = oldCount + 1
      val s = "INSERT INTO `COUNTER` (`VALUE`) VALUES (?)"
      val stmt = c.prepareStatement(s)
      stmt.setInt(1, count)
      stmt.executeUpdate
      stmt.close
      count

    val oldCount = getOldCount(c)
    dropCount(c)
    val newCount = addCount(oldCount)(c)

    newCount

  def apply[F[_]: Transactor](c: Connection): Counter[F] =

    unsafeInit(c)

    new Counter:
      override def incrementAndGet(): F[Int] =
        summon[Transactor[F]].transact { c =>
          unsafeIncrementAndGet(c)
        }
