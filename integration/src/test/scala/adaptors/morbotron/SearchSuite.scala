package sectery.adaptors.morbotron

import munit.FunSuite
import sectery._
import sectery.adaptors._
import sectery.effects._
import sectery.effects.id._
import sectery.effects.id.given

class SearchSuite extends FunSuite:

  given logger: Logger[Id] with
    override def debug(message: => String) =
      println(message)
    override def error(message: => String) =
      println(message)

  given httpClient: HttpClient[Id] =
    new LiveHttpClient[Id]

  test("search"):

    val obtained: Option[Search.Search] =
      Search.search("windmills do not work that way")

    val expected: Option[Search.Search] =
      Some(
        Search.Search(
          Id = 2987490,
          Episode = "S05E01",
          Timestamp = 377835
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
