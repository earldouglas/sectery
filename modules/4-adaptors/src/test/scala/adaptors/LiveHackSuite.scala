package sectery.adaptors

import java.sql.Connection
import java.sql.DriverManager
import munit.FunSuite
import sectery._
import sectery.effects._
import sectery.effects.id._

class LiveHackSuite extends FunSuite:

  val c: Connection =
    Class.forName("org.h2.Driver")
    DriverManager.getConnection(s"jdbc:h2:mem:;MODE=MYSQL")

  given transactor: Transactor[Id] with
    override def transact[A](k: Connection => A) = k(c)

  test("Valid words") {

    assertEquals(
      obtained = LiveHack(c).isAWord("foo"),
      expected = false
    )

    val s = "INSERT INTO HACK_WORDS (WORD) VALUES (?)"
    val stmt = c.prepareStatement(s)
    stmt.setString(1, "foo")
    stmt.executeUpdate()
    stmt.close()

    assertEquals(
      obtained = LiveHack(c).isAWord("foo"),
      expected = true
    )
  }

  test("Start new game") {
    assertEquals(
      obtained = LiveHack(c).getOrStartGame("irc", "#foo"),
      expected = ("foo", 0)
    )
  }

  test("Take a buncha guesses") {

    assertEquals(
      obtained = LiveHack(c).setGuessCount("irc", "#foo", 42),
      expected = ()
    )

    assertEquals(
      obtained = LiveHack(c).getOrStartGame("irc", "#foo"),
      expected = ("foo", 42)
    )
  }

  test("Start another new game") {

    assertEquals(
      obtained = LiveHack(c).deleteGame("irc", "#foo"),
      expected = ()
    )

    assertEquals(
      obtained = LiveHack(c).getOrStartGame("irc", "#foo"),
      expected = ("foo", 0)
    )
  }
