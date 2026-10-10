package sectery.adaptors.frinkiac

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
      Search.search("i got to think of a lie fast")

    val expected: Option[Search.Search] =
      Some(
        Search.Search(
          Id = 3514571,
          Episode = "S04E16",
          Timestamp = 164915
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
