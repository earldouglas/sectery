package sectery.adaptors.wx

import munit.FunSuite
import sectery._
import sectery.adaptors._
import sectery.effects._
import sectery.effects.id._
import sectery.effects.id.given

class OpenWeatherMapSuite extends FunSuite:

  given logger: Logger[Id] with
    override def debug(message: => String) =
      println(message)
    override def error(message: => String) =
      println(message)

  given httpClient: HttpClient[Id] =
    new LiveHttpClient[Id]

  test("findCurrent"):

    val obtained: Option[OpenWeatherMap.Current] =
      OpenWeatherMap.findCurrent(
        apiKey = sys.env("OPEN_WEATHER_MAP_API_KEY"),
        lat = 37.7879363d,
        lon = -122.4075201d
      )

    assert(obtained.get.temp >= 0)
    assert(obtained.get.humidity >= 0)
    assert(obtained.get.wind_speed >= 0)
    assert(obtained.get.uvi >= 0)
