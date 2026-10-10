package sectery.adaptors.wx

import java.net.URI
import java.net.URLEncoder
import sectery._
import sectery.control.Monad
import sectery.control.Monad._
import sectery.effects.HttpClient.Response
import sectery.effects._
import zio.json._

object OSM:

  case class PlaceJson(
      display_name: String,
      lat: Double,
      lon: Double
  )

  object PlaceJson:
    implicit val decoder: JsonDecoder[PlaceJson] =
      DeriveJsonDecoder.gen[PlaceJson]

  case class Place(
      displayName: String,
      shortName: String,
      lat: Double,
      lon: Double
  )

  def findPlace[F[_]: HttpClient: Monad: Logger](
      q: String
  ): F[Option[Place]] =

    val qEnc = URLEncoder.encode(q, "UTF-8")

    val logger: Logger[F] = summon

    summon[HttpClient[F]]
      .request(
        method = "GET",
        url = new URI(
          s"""https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${qEnc}"""
        ).toURL(),
        headers = Map("Accept" -> "application/json"),
        body = None
      )
      .map:
        case Response(200, _, body) =>
          body.fromJson[List[PlaceJson]] match
            case Right(pjs) =>
              pjs.headOption.map(pj =>
                Place(
                  displayName = pj.display_name,
                  shortName = pj.display_name
                    .split(", ")
                    .headOption
                    .getOrElse(q),
                  lat = pj.lat,
                  lon = pj.lon
                )
              )
            case Left(e) =>
              logger.error(s"error ${e} parsing json: ${body}")
              None
        case response =>
          logger.error(s"unexpected response: ${response}")
          None
