package sectery.adaptors

import java.net.URI
import java.net.URLEncoder
import sectery._
import sectery.control.Monad
import sectery.control.Monad._
import sectery.effects.HttpClient.Response
import sectery.effects._
import zio.json._

object LiveGeode:

  implicit val decoder: JsonDecoder[Geode.Location] =
    DeriveJsonDecoder.gen[Geode.Location]

  def apply[F[_]: HttpClient: Monad: Logger](url: String): Geode[F] =
    val urlNoSlash = url.replaceAll("""\/$""", "")
    new Geode:
      override def geode(
          ip: String
      ): F[Either[String, Geode.Location]] =
        val encIp = URLEncoder.encode(ip, "UTF-8")
        summon[HttpClient[F]]
          .request(
            method = "GET",
            url = new URI(s"${urlNoSlash}/${encIp}").toURL(),
            headers = Map.empty,
            body = None
          )
          .map {
            case Response(200, _, body) =>
              body.fromJson[Geode.Location] match
                case Right(location) =>
                  Right(location)
                case Left(e) =>
                  summon[Logger[F]].error(
                    s"can't parse geode location for ip ${ip}: ${e}"
                  )
                  Left("can't figure that one out")
            case x =>
              summon[Logger[F]].error(
                s"unsupported geode response for ip ${ip}: ${x}"
              )
              Left("can't figure that one out")
          }
