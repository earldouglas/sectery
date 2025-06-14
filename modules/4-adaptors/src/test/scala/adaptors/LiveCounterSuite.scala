package sectery.adaptors

import java.sql.Connection
import java.sql.DriverManager
import munit.FunSuite
import sectery.effects._
import sectery.effects.id._

class LiveCounterSuite extends FunSuite:

  test("Increment and get a coupla times") {

    val c: Connection =
      Class.forName("org.h2.Driver")
      DriverManager.getConnection(s"jdbc:h2:mem:;MODE=MYSQL")

    given transactor: Transactor[Id] =
      new Transactor:
        override def transact[A](k: Connection => A): Id[A] =
          k(c)

    assertEquals(
      obtained = LiveCounter(c).incrementAndGet(),
      expected = 1
    )

    assertEquals(
      obtained = LiveCounter(c).incrementAndGet(),
      expected = 2
    )
  }
