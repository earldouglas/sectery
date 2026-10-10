package sectery.adaptors.morbotron

import java.net.URI
import java.net.URLEncoder
import sectery._
import sectery.control.Monad
import sectery.control.Monad._
import sectery.effects.HttpClient.Response
import sectery.effects._
import zio.json._

object Search:

  case class Search(
      Id: Long,
      Episode: String,
      Timestamp: Long
  )

  object Search:
    implicit val decoder: JsonDecoder[Search] =
      DeriveJsonDecoder.gen[Search]

  def search[F[_]: HttpClient: Monad: Logger](
      q: String
  ): F[Option[Search]] =

    val logger: Logger[F] = summon

    val qEnc = URLEncoder.encode(q, "UTF-8")

    summon[HttpClient[F]]
      .request(
        method = "GET",
        url = new URI(
          s"""https://morbotron.com/api/search?q=${qEnc}"""
        ).toURL(),
        headers = Map("Accept" -> "application/json"),
        body = None
      )
      .map:
        case Response(200, _, body) =>
          body.fromJson[List[Search]] match
            case Right(ss) => ss.headOption
            case Left(e) =>
              logger.error(s"Error ${e} parsing json ${body}")
              None
        case response =>
          logger.error(s"Unexpected response ${response}")
          None
