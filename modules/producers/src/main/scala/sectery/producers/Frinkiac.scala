package sectery.producers

import java.net.URLEncoder
import scala.jdk.CollectionConverters._
import sectery.Http
import sectery.Producer
import sectery.Response
import sectery.Rx
import sectery.Tx
import zio.ZIO
import zio.json._

object Frinkiac extends Producer:

  case class ZIOO[R, E, A](z: ZIO[R, E, Option[A]]):

    def map[B](f: A => B): ZIO[R, E, Option[B]] =
      z.map(_.map(f))

    def flatMap[B](f: A => ZIO[R, E, Option[B]]): ZIO[R, E, Option[B]] =
      z.flatMap(_.map(f).getOrElse(ZIO.succeed(None)))

  object Search:

    case class Search(
        Id: Long,
        Episode: String,
        Timestamp: Long
    )

    object Search:
      implicit val decoder: JsonDecoder[Search] =
        DeriveJsonDecoder.gen[Search]

    def search(q: String): ZIO[Http.Http, Throwable, List[Search]] =
      val qEnc = URLEncoder.encode(q, "UTF-8")
      Http
        .request(
          method = "GET",
          url = s"""https://frinkiac.com/api/search?q=${qEnc}""",
          headers =
            Map("User-Agent" -> "bot", "Accept" -> "application/json"),
          body = None
        )
        .flatMap { case Response(200, _, body) =>
          body.fromJson[List[Search]] match
            case Right(ss) => ZIO.succeed(ss)
            case Left(_)   => ZIO.succeed(Nil)
        }

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
    ): ZIO[Http.Http, Throwable, Option[Caption]] =
      Http
        .request(
          method = "GET",
          url = s"""https://frinkiac.com/api/caption?e=${e}&t=${t}""",
          headers =
            Map("User-Agent" -> "bot", "Accept" -> "application/json"),
          body = None
        )
        .flatMap { case Response(200, _, body) =>
          body.fromJson[Caption] match
            case Right(c) => ZIO.succeed(Some(c))
            case Left(_)  => ZIO.succeed(None)
        }

  private val frinkiac = """^@frinkiac\s+(.+)\s*$""".r

  override def help(): Iterable[Info] =
    Some(
      Info(
        "@frinkiac",
        "@frinkiac <quote>, e.g. @frinkiac i got to think of a lie fast"
      )
    )

  override def apply(m: Rx): ZIO[Http.Http, Throwable, Iterable[Tx]] =
    m match
      case Rx(channel, _, frinkiac(q)) =>
        (
          for
            s <- ZIOO(Search.search(q).map(_.headOption))
            c <- ZIOO(Caption.caption(s.Episode, s.Timestamp))
          yield Tx(
            channel,
            s"https://frinkiac.com/caption/${s.Episode}/${s.Timestamp}"
          ) :: c.Subtitles
            .map(_.Content)
            .map(content => Tx(channel, content))
        ).map(_.toList.flatten)
      case _ =>
        ZIO.succeed(None)
