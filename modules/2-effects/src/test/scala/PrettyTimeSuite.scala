package sectery

import munit.FunSuite
import java.time.Instant

class PrettyTimeSuite extends FunSuite:

  test("Use DST in the Summer") {
    val summer = Instant.EPOCH.plusSeconds(60 * 60 * 24 * 30 * 6)
    assertEquals(
      obtained = PrettyTime(summer, "America/Los_Angeles"),
      expected = "Mon, 29 Jun 1970, 17:00 PDT"
    )
    assertEquals(
      obtained = PrettyTime(summer, "PST"),
      expected = "Mon, 29 Jun 1970, 17:00 PDT"
    )
  }

  test("Don't use DST in the Winter") {

    assertEquals(
      obtained = PrettyTime(Instant.EPOCH, "America/Phoenix"),
      expected = "Wed, 31 Dec 1969, 17:00 MST"
    )

    assertEquals(
      obtained = PrettyTime(Instant.EPOCH, "America/New_York"),
      expected = "Wed, 31 Dec 1969, 19:00 EST"
    )
  }

  test("Use GMT for bogus time zones") {
    assertEquals(
      obtained = PrettyTime(Instant.EPOCH, "Foo/Bar_Baz"),
      expected = "Thu, 1 Jan 1970, 24:00 GMT"
    )
  }
