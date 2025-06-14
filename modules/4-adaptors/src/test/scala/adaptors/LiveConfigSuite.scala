package sectery.adaptors

import java.sql.Connection
import java.sql.DriverManager
import munit.FunSuite
import sectery.effects._
import sectery.effects.id._

class LiveConfigSuite extends FunSuite:

  test("Set and then get config") {

    val c: Connection =
      Class.forName("org.h2.Driver")
      DriverManager.getConnection(s"jdbc:h2:mem:;MODE=MYSQL")

    given transactor: Transactor[Id] =
      new Transactor:
        override def transact[A](k: Connection => Id[A]) = k(c)

    assertEquals(
      obtained = LiveConfig(c).getConfig("bar", "baz"),
      expected = None
    )

    assertEquals(
      obtained = LiveConfig(c).setConfig("bar", "baz", "raz"),
      expected = ()
    )

    assertEquals(
      obtained = LiveConfig(c).getConfig("bar", "baz"),
      expected = Some("raz")
    )
  }

  test("Attempt to get non-existing config") {

    val c: Connection =
      Class.forName("org.h2.Driver")
      DriverManager.getConnection(s"jdbc:h2:mem:;MODE=MYSQL")

    given transactor: Transactor[Id] =
      new Transactor:
        override def transact[A](k: Connection => Id[A]) = k(c)

    assertEquals(
      obtained = LiveConfig(c).getConfig("bar", "baz"),
      expected = None
    )

    assertEquals(
      obtained = LiveConfig(c).getConfig("bar", "qux"),
      expected = None
    )

    assertEquals(
      obtained = LiveConfig(c).getConfig("quz", "baz"),
      expected = None
    )
  }
