package sectery.adaptors.wx

import java.net.URI
import sectery._
import sectery.control.Monad
import sectery.control.Monad._
import sectery.effects.HttpClient.Response
import sectery.effects._
import zio.json._

object AirNowObservation:

  case class Category(
      Name: String
  )

  object Category:
    implicit val decoder: JsonDecoder[Category] =
      DeriveJsonDecoder.gen[Category]

  case class Observation(
      ParameterName: String,
      AQI: Int,
      Category: Category
  )

  object Observation:
    implicit val decoder: JsonDecoder[Observation] =
      DeriveJsonDecoder.gen[Observation]

  case class AqiParameter(
      name: String,
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
          s"""https://www.airnowapi.org/aq/observation/latLong/current/?format=application/json&latitude=${lat}&longitude=${lon}&distance=50&API_KEY=${apiKey}"""
        ).toURL(),
        headers = Map(
          "User-Agent" -> "bot",
          "Accept" -> "application/json"
        ),
        body = None
      )
      .map:
        case Response(200, _, body) =>
          body.fromJson[List[Observation]] match
            case Right(os) =>
              Some(
                Aqi(
                  parameters = os.map { o =>
                    AqiParameter(
                      name = o.ParameterName,
                      value = o.AQI,
                      category = o.Category.Name
                    )
                  }
                )
              )
            case Left(e) =>
              logger.error(s"error ${e} parsing json: ${body}")
              None
        case response =>
          logger.error(s"unexpected response: ${response}")
          None
