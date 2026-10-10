package sectery.adaptors.wx

import munit.FunSuite
import sectery._
import sectery.adaptors._
import sectery.effects._
import sectery.effects.id._
import sectery.effects.id.given

class AirNowObservationSuite extends FunSuite:

  given logger: Logger[Id] with
    override def debug(message: => String) =
      println(message)
    override def error(message: => String) =
      println(message)

  given httpClient: HttpClient[Id] =
    new LiveHttpClient[Id]

  test("findAqi"):

    val obtained: Option[wx.AirNowObservation.Aqi] =
      wx.AirNowObservation.findAqi(
        apiKey = sys.env("AIRNOW_API_KEY"),
        lat = 37.7879363d,
        lon = -122.4075201d
      )

    assert(obtained.get.parameters.size > 0)
