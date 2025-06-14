package sectery.adaptors

import java.net.URI
import java.net.URLEncoder
import sectery._
import sectery.control.Monad
import sectery.control.Monad._
import sectery.effects.HttpClient.Response
import sectery.effects._
import zio.json._

object LiveFrinkiac:

  def apply[F[_]: HttpClient: Monad](): Frinkiac[F] =
    new Frinkiac:

      object Search:

        case class Search(
            Id: Long,
            Episode: String,
            Timestamp: Long
        )

        object Search:
          implicit val decoder: JsonDecoder[Search] =
            DeriveJsonDecoder.gen[Search]

        def search(q: String): F[Option[Search]] =
          val qEnc = URLEncoder.encode(q, "UTF-8")
          summon[HttpClient[F]]
            .request(
              method = "GET",
              url = new URI(
                s"""https://frinkiac.com/api/search?q=${qEnc}"""
              ).toURL(),
              headers = Map("Accept" -> "application/json"),
              body = None
            )
            .map:
              case Response(200, _, body) =>
                body.fromJson[List[Search]] match
                  case Right(ss) => ss.headOption
                  case Left(_)   => None
              case _ => None

      object Caption:

        case class Episode(
            Id: Long,
            Key: String,
            Season: Long,
            EpisodeNumber: Long,
            Title: String,
            Director: String,
            Writer: String,
            OriginalAirDate: String,
            WikiLink: String
        )

        object Episode:
          implicit val decoder: JsonDecoder[Episode] =
            DeriveJsonDecoder.gen[Episode]

        case class Frame(
            Id: Long,
            Episode: String,
            Timestamp: Long
        )

        object Frame:
          implicit val decoder: JsonDecoder[Frame] =
            DeriveJsonDecoder.gen[Frame]

        case class Subtitles(
            Id: Long,
            RepresentativeTimestamp: Long,
            Episode: String,
            StartTimestamp: Long,
            EndTimestamp: Long,
            Content: String,
            Language: String
        )

        object Subtitles:
          implicit val decoder: JsonDecoder[Subtitles] =
            DeriveJsonDecoder.gen[Subtitles]

        case class Nearby(
            Id: Long,
            Episode: String,
            Timestamp: Long
        )

        object Nearby:
          implicit val decoder: JsonDecoder[Nearby] =
            DeriveJsonDecoder.gen[Nearby]

        case class Caption(
            Episode: Episode,
            Frame: Frame,
            Subtitles: List[Subtitles],
            Nearby: List[Nearby]
        )

        object Caption:
          implicit val decoder: JsonDecoder[Caption] =
            DeriveJsonDecoder.gen[Caption]

        def caption(
            e: String,
            t: Long
        ): F[Option[Caption]] =
          summon[HttpClient[F]]
            .request(
              method = "GET",
              url = new URI(
                s"""https://frinkiac.com/api/caption?e=${e}&t=${t}"""
              ).toURL(),
              headers = Map(
                "User-Agent" -> "bot",
                "Accept" -> "application/json"
              ),
              body = None
            )
            .map:
              case Response(200, _, body) =>
                body.fromJson[Caption] match
                  case Right(c) => Some(c)
                  case Left(_)  => None
              case _ => None

      override def frinkiac(q: String): F[List[String]] =
        for
          so <- Search.search(q)
          co <- so match {
            case Some(s) =>
              Caption.caption(s.Episode, s.Timestamp)
            case None =>
              summon[Monad[F]].pure(None)
          }
        yield
          val link: List[String] =
            so.toList.map { s =>
              s"https://frinkiac.com/caption/${s.Episode}/${s.Timestamp}"
            }
          val caption: List[String] =
            co.toList.flatMap { c =>
              c.Subtitles.map(_.Content)
            }
          link ++ caption
