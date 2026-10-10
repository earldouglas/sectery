package sectery.adaptors.wx

import java.net.URI
import sectery._
import sectery.control.Monad
import sectery.control.Monad._
import sectery.effects.HttpClient.Response
import sectery.effects._
import zio.json._

object OpenWeatherMap:

  case class OneCall(
      current: Current
  )

  object OneCall:
    implicit val decoder: JsonDecoder[OneCall] =
      DeriveJsonDecoder.gen[OneCall]

  case class Weather(
      main: String,
      description: String
  )

  object Weather:
    implicit val decoder: JsonDecoder[Weather] =
      DeriveJsonDecoder.gen[Weather]

  case class Current(
      temp: Float,
      humidity: Float,
      wind_speed: Float,
      uvi: Float,
      weather: List[Weather]
  )

  object Current:
    implicit val decoder: JsonDecoder[Current] =
      DeriveJsonDecoder.gen[Current]

  def findCurrent[F[_]: HttpClient: Monad: Logger](
      apiKey: String,
      lat: Double,
      lon: Double
  ): F[Option[Current]] =

    val logger: Logger[F] = summon

    summon[HttpClient[F]]
      .request(
        method = "GET",
        url = new URI(
          s"""https://api.openweathermap.org/data/3.0/onecall?lat=${lat}&lon=${lon}&exclude=minutely,hourly,daily,alerts&units=imperial&appid=${apiKey}"""
        ).toURL(),
        headers = Map(
          "User-Agent" -> "bot",
          "Accept" -> "application/json"
        ),
        body = None
      )
      .map:
        case Response(200, _, body) =>
          body.fromJson[OneCall] match
            case Right(x) =>
              Some(x.current)
            case Left(e) =>
              logger.error(s"error ${e} parsing json: ${body}")
              None
        case response =>
          logger.error(s"unexpected response: ${response}")
          None
