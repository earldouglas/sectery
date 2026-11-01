package sectery.adaptors

import com.earldouglas.outdent.Outdent.outdent
import java.net.URL
import munit.FunSuite
import sectery._
import sectery.effects.HttpClient.Response
import sectery.effects._
import sectery.effects.id._
import sectery.effects.id.given

class LiveGeodeSuite extends FunSuite:

  given http: HttpClient[Id] =
    new HttpClient:
      override def request(
          method: String,
          url: URL,
          headers: Map[String, String],
          body: Option[String]
      ): Id[Response] =
        url.toString() match
          case "https://geode/8.8.4.4" =>
            Response(
              status = 200,
              headers = Map.empty,
              body = """
                {
                  "city": "Westfield",
                  "continent": "North America",
                  "continentCode": "NA",
                  "countryCode": "US",
                  "countryName": "United States",
                  "latitude": 42.1293,
                  "longitude": -72.7522,
                  "postalCode": "01085",
                  "region": "MA",
                  "regionName": "Massachusetts"
                }
              """.outdent
            )
          case "https://geode/127.0.0.1" =>
            Response(
              status = 404,
              headers = Map.empty,
              body = "Information for address does not exist."
            )

  val geode: Geode[Id] = LiveGeode("https://geode/")

  test("@geode 8.8.4.4") {

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

    assertEquals(
      obtained = geode.geode("8.8.4.4"),
      expected = Right(location)
    )
  }

  test("@geode 127.0.0.1") {
    assertEquals(
      obtained = geode.geode("127.0.0.1"),
      expected = Left(
        "unexpected response: Response(404,Map(),Information for address does not exist.)"
      )
    )
  }
