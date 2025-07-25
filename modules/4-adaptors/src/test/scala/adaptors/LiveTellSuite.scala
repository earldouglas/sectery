package sectery.adaptors

import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant
import munit.FunSuite
import sectery.effects._
import sectery.effects.id._

class LiveTellSuite extends FunSuite:

  test("Increment and get a coupla times") {

    val c: Connection =
      Class.forName("org.h2.Driver")
      DriverManager.getConnection(s"jdbc:h2:mem:;MODE=MYSQL")

    given transactor: Transactor[Id] =
      new Transactor:
        override def transact[A](k: Connection => A): Id[A] =
          k(c)

    LiveTell(c).save(
      "irc",
      "#foo",
      Tell.Saved(
        to = "jdoe",
        from = "bar",
        message = "heyo",
        date = Instant.EPOCH
      )
    )

    assertEquals(
      obtained = LiveTell(c).pop("irc", "#foo", "jdoe"),
      expected = List(
        Tell.Saved(
          to = "jdoe",
          from = "bar",
          message = "heyo",
          date = Instant.EPOCH
        )
      )
    )
  }
