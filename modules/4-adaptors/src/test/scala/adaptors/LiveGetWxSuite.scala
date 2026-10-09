package sectery.adaptors

import java.net.URL
import munit.FunSuite
import sectery._
import sectery.effects.HttpClient.Response
import sectery.effects._
import sectery.effects.id._
import sectery.effects.id.given

class LiveGetWxSuite extends FunSuite:

  given logger: Logger[Id] with
    override def debug(message: => String) =
      println(message)
    override def error(message: => String) =
      println(message)

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
          case "https://www.airnowapi.org/aq/observation/current/ziplatLong/?format=application/json&latitude=37.7790262&longitude=-122.419906&distance=50&API_KEY=alligator3" =>
            Response(
              status = 200,
              headers = Map.empty,
              body = Resource.read(
                "/org/airnowapi/www/aq/observation/current/ziplatLong/?format=application/json&latitude=37.7790262&longitude=-122.419906&distance=50&API_KEY=alligator3"
              )
            )
          case s"""https://www.airnowapi.org/aq/forecast/current/?format=application/json&latitude=37.7790262&longitude=-122.419906&distance=50&API_KEY=alligator3""" =>
            Response(
              status = 200,
              headers = Map.empty,
              body = Resource.read(
                "/org/airnowapi/www/aq/forecast/current/?format=application/json&latitude=37.7790262&longitude=-122.419906&distance=50&API_KEY=alligator3"
              )
            )

  test("Retrieve weather for san francisco") {

    val obtained: String =
      LiveGetWx("alligator3", "alligator3")
        .getWx("san francisco")

    val expected: String =
      "San Francisco: 44°, hum 65%, wnd 10 mph, few clouds, uv 0, ozone 21, pm2.5 34"

    assertEquals(
      obtained = obtained.toList,
      expected = expected.toList
    )
  }
