package sectery.adaptors.wx

import java.net.URI
import sectery._
import sectery.control.Monad
import sectery.control.Monad._
import sectery.effects.HttpClient.Response
import sectery.effects._
import zio.json._

object DarkSky:

  case class Currently(
      temperature: Float,
      humidity: Float,
      windSpeed: Float,
      windGust: Float,
      uvIndex: Int
  )

  object Currently:
    implicit val decoder: JsonDecoder[Currently] =
      DeriveJsonDecoder.gen[Currently]

  case class Data(
      temperatureHigh: Float,
      temperatureLow: Float
  )

  object Data:
    implicit val decoder: JsonDecoder[Data] =
      DeriveJsonDecoder.gen[Data]

  case class Daily(
      data: List[Data]
  )

  object Daily:
    implicit val decoder: JsonDecoder[Daily] =
      DeriveJsonDecoder.gen[Daily]

  case class ForecastJson(
      currently: Currently,
      daily: Daily
  )

  object ForecastJson:
    implicit val decoder: JsonDecoder[ForecastJson] =
      DeriveJsonDecoder.gen[ForecastJson]

  case class Forecast(
      temperature: Double,
      temperatureHigh: Double,
      temperatureLow: Double,
      humidity: Double,
      wind: Double,
      gusts: Double,
      uvIndex: Int
  )

  def findForecast[F[_]: HttpClient: Monad: Logger](
      apiKey: String,
      lat: Double,
      lon: Double
  ): F[Option[Forecast]] =

    val logger: Logger[F] = summon

    summon[HttpClient[F]]
      .request(
        method = "GET",
        url = new URI(
          s"""https://api.darksky.net/forecast/${apiKey}/${lat},${lon}"""
        ).toURL(),
        headers = Map(
          "User-Agent" -> "bot",
          "Accept" -> "application/json"
        ),
        body = None
      )
      .map:
        case Response(200, _, body) =>
          body.fromJson[ForecastJson] match
            case Right(fj) =>
              fj.daily.data.headOption.map { dailyData =>
                Forecast(
                  temperature = fj.currently.temperature,
                  temperatureHigh = dailyData.temperatureHigh,
                  temperatureLow = dailyData.temperatureLow,
                  humidity = fj.currently.humidity * 100,
                  wind = fj.currently.windSpeed,
                  gusts = fj.currently.windGust,
                  uvIndex = fj.currently.uvIndex
                )
              }
            case Left(e) =>
              logger.error(s"error ${e} parsing json: ${body}")
              None
        case response =>
          logger.error(s"unexpected response: ${response}")
          None
