package sectery.usecases.responders

import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class GeodeResponderSuite extends FunSuite:

  test("@geode <ip> produces result") {

    val location: Geode.Location =
      Geode.Location(
        city = Some("Westfield"),
        continent = Some("North America"),
        continentCode = Some("NA"),
        countryCode = Some("US"),
        countryName = Some("United States"),
        latitude = Some(42.1293f),
        longitude = Some(-72.7522f),
        postalCode = Some("01085"),
        region = Some("MA"),
        regionName = Some("Massachusetts")
      )

    given haskgeode: Geode[Id] with
      override def geode(
          src: String
      ): Id[Either[String, Geode.Location]] =
        src match {
          case "8.8.4.4" =>
            Right(location)
        }

    val obtained: List[Tx] =
      new GeodeResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@geode 8.8.4.4")
        )

    val expected: List[Tx] =
      List(Tx("irc", "#foo", None, location.toString()))

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }
