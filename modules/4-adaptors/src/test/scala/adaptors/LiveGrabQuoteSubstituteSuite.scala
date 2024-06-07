package sectery.adaptors

import sectery.effects.Quote.GrabbedMessage
import java.sql.Connection
import java.sql.DriverManager
import munit.FunSuite
import sectery._
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id._
import sectery.effects.id.given
import java.time.Instant

class LiveGrabQuoteSubstituteSuite extends FunSuite:

  val c: Connection =
    Class.forName("org.h2.Driver")
    DriverManager.getConnection(s"jdbc:h2:mem:;MODE=MYSQL")

  given transactor: Transactor[Id] with
    override def transact[A](k: Connection => A) = k(c)

  val zero: Instant =
    Instant.EPOCH

  test("Save a couple of messages") {

    {
      given now: Now[Id] with
        override def now(): Id[Instant] =
          zero.minusSeconds(2)

      assertEquals(
        obtained = LiveGrabQuoteSubstitute(c).saveLastMessage(
          Rx("irc", "#foo", None, "bar", "yo")
        ),
        expected = ()
      )
    }

    {
      given now: Now[Id] with
        override def now(): Id[Instant] =
          zero.minusSeconds(1)

      assertEquals(
        obtained = LiveGrabQuoteSubstitute(c).saveLastMessage(
          Rx("irc", "#foo", None, "jdoe", "word")
        ),
        expected = ()
      )
    }
  }

  test("Nothing is grabbed yet") {

    given now: Now[Id] with
      override def now(): Id[Instant] =
        zero

    assertEquals(
      obtained = LiveGrabQuoteSubstitute(c).quote("irc", "#foo", "bar"),
      expected = None
    )

    assertEquals(
      obtained =
        LiveGrabQuoteSubstitute(c).quote("irc", "#foo", "jdoe"),
      expected = None
    )

    assertEquals(
      obtained = LiveGrabQuoteSubstitute(c).quote("irc", "#foo"),
      expected = None
    )
  }

  test("Grab the channel") {

    given now: Now[Id] with
      override def now(): Id[Instant] =
        zero

    assertEquals(
      obtained = LiveGrabQuoteSubstitute(c).grab("irc", "#foo", "jdoe"),
      expected = true
    )

    assertEquals(
      obtained = LiveGrabQuoteSubstitute(c).quote("irc", "#foo", "bar"),
      expected = None
    )

    assertEquals(
      obtained =
        LiveGrabQuoteSubstitute(c).quote("irc", "#foo", "jdoe"),
      expected = Some(
        GrabbedMessage(
          service = "irc",
          channel = "#foo",
          nick = "jdoe",
          message = "word",
          instant = Instant.EPOCH.minusSeconds(1)
        )
      )
    )

    assertEquals(
      obtained = LiveGrabQuoteSubstitute(c).quote("irc", "#foo"),
      expected = Some(
        GrabbedMessage(
          service = "irc",
          channel = "#foo",
          nick = "jdoe",
          message = "word",
          instant = Instant.EPOCH.minusSeconds(1)
        )
      )
    )
  }

  test("Grab bar") {

    given now: Now[Id] with
      override def now(): Id[Instant] =
        zero

    assertEquals(
      obtained = LiveGrabQuoteSubstitute(c).grab("irc", "#foo", "bar"),
      expected = true
    )

    assertEquals(
      obtained = LiveGrabQuoteSubstitute(c).quote("irc", "#foo", "bar"),
      expected = Some(
        GrabbedMessage(
          service = "irc",
          channel = "#foo",
          nick = "bar",
          message = "yo",
          instant = Instant.EPOCH.minusSeconds(2)
        )
      )
    )

    assertEquals(
      obtained =
        LiveGrabQuoteSubstitute(c).quote("irc", "#foo", "jdoe"),
      expected = Some(
        GrabbedMessage(
          service = "irc",
          channel = "#foo",
          nick = "jdoe",
          message = "word",
          instant = Instant.EPOCH.minusSeconds(1)
        )
      )
    )

    assert(LiveGrabQuoteSubstitute(c).quote("irc", "#foo").isDefined)
  }
