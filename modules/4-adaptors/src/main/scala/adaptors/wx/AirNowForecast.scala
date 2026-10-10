package sectery.adaptors.wx

import java.net.URI
import sectery._
import sectery.control.Monad
import sectery.control.Monad._
import sectery.effects.HttpClient.Response
import sectery.effects._
import zio.json._

object AirNowForecast:

  case class Forecast(
      dateValid: String,
      parameterName: String,
      aqi: Int,
      categoryName: String
  )

  object Forecast:
    implicit val decoder: JsonDecoder[Forecast] =
      DeriveJsonDecoder.gen[Forecast]

  case class AqiParameter(
      name: String,
      date: String,
      value: Int,
      category: String
  )
  case class Aqi(parameters: List[AqiParameter])

  def findAqi[F[_]: HttpClient: Monad: Logger](
      apiKey: String,
      lat: Double,
      lon: Double
  ): F[Option[Aqi]] =

    val logger: Logger[F] = summon

    summon[HttpClient[F]]
      .request(
        method = "GET",
        url = new URI(
          s"""https://www.airnowapi.org/aq/forecast/current/?format=application/json&latitude=${lat}&longitude=${lon}&distance=50&API_KEY=${apiKey}"""
        ).toURL(),
        headers = Map(
          "User-Agent" -> "bot",
          "Accept" -> "application/json"
        ),
        body = None
      )
      .map:
        case Response(200, _, body) =>
          body.fromJson[List[Forecast]] match
            case Right(fs) =>
              if fs.length > 0 then
                Some(
                  Aqi(
                    parameters = fs.map { f =>
                      AqiParameter(
                        name = f.parameterName,
                        date = f.dateValid,
                        value = f.aqi,
                        category = f.categoryName
                      )
                    }
                  )
                )
              else None
            case Left(e) =>
              logger.error(s"error ${e} parsing json: ${body}")
              None
        case response =>
          logger.error(s"unexpected response: ${response}")
          None
