package sectery.adaptors

import java.net.URL
import munit.FunSuite
import sectery._
import sectery.effects.HttpClient.Response
import sectery.effects._
import sectery.effects.id._
import sectery.effects.id.given

class LiveGetWxSuite extends FunSuite:

  given httpClient: HttpClient[Id] =
    new HttpClient:
      override def request(
          method: String,
          url: URL,
          headers: Map[String, String],
          body: Option[String]
      ): Id[Response] =
        url.toString() match
          case "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=san+francisco" =>
            Response(
              status = 200,
              headers = Map.empty,
              body = Resource.read(
                "/org/openstreetmap/nominatim/search?format=json&limit=1&q=san+francisco"
              )
            )
          case "https://api.darksky.net/forecast/alligator3/37.7790262,-122.419906" =>
            Response(
              status = 200,
              headers = Map.empty,
              body = Resource.read(
                "/net/darksky/api/forecast/alligator3/37.7790262,-122.419906"
              )
            )
          case "https://api.openweathermap.org/data/3.0/onecall?lat=37.7790262&lon=-122.419906&exclude=minutely,hourly,daily,alerts&units=imperial&appid=alligator3" =>
            Response(
              status = 200,
              headers = Map.empty,
              body = Resource.read(
                "/org/openweathermap/api/data/3.0/onecall?lat=37.7790262&lon=-122.419906&exclude=minutely,hourly,daily,alerts&units=imperial&appid=alligator3"
              )
            )
          case "https://www.airnowapi.org/aq/observation/latLong/current/?format=application/json&latitude=37.7790262&longitude=-122.419906&distance=50&API_KEY=alligator3" =>
            Response(
              status = 200,
              headers = Map.empty,
              body = Resource.read(
                "/org/airnowapi/www/aq/observation/latLong/current/?format=application/json&latitude=37.7790262&longitude=-122.419906&distance=50&API_KEY=alligator3"
              )
            )
          case s"""https://www.airnowapi.org/aq/forecast/latLong/?format=application/json&latitude=37.7790262&longitude=-122.419906&distance=50&API_KEY=alligator3""" =>
            Response(
              status = 200,
              headers = Map.empty,
              body = Resource.read(
                "/org/airnowapi/www/aq/forecast/latLong/?format=application/json&latitude=37.7790262&longitude=-122.419906&distance=50&API_KEY=alligator3"
              )
            )

  test("Retrieve weather for san francisco") {

    given getConfig: GetConfig[Id] with
      override def getConfig(
          nick: String,
          key: String
      ): Id[Option[String]] =
        None

    val obtained: String =
      LiveGetWx("alligator3", "alligator3")
        .getWx("san francisco")

    val expected: String =
      "San Francisco: 44°, hum 65%, wnd 10 mph, few clouds, uv 0, o3 15, pm2.5 0"

    assertEquals(
      obtained = obtained.toList,
      expected = expected.toList
    )
  }
