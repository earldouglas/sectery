package sectery.adaptors

import java.sql.Connection
import java.sql.DriverManager
import munit.FunSuite
import sectery._
import sectery.effects._
import sectery.effects.id._

class LivePointsSuite extends FunSuite:

  val c: Connection =
    Class.forName("org.h2.Driver")
    DriverManager.getConnection(s"jdbc:h2:mem:;MODE=MYSQL")

  given transactor: Transactor[Id] with
    override def transact[A](k: Connection => A) = k(c)

  test("Update") {

    assertEquals(
      obtained = LivePoints(c).update("irc", "#foo", "jdoe", 0),
      expected = 0L
    )

    assertEquals(
      obtained = LivePoints(c).update("irc", "#foo", "jdoe", 1),
      expected = 1L
    )

    assertEquals(
      obtained = LivePoints(c).update("irc", "#foo", "jdoe", 1),
      expected = 2L
    )

    assertEquals(
      obtained = LivePoints(c).update("irc", "#foo", "jdoe", 40),
      expected = 42L
    )
  }
