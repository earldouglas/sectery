package sectery.producers

import java.net.URLEncoder
import org.json4s.JsonDSL._
import org.json4s.MonadicJValue.jvalueToMonadic
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import scala.collection.JavaConverters._
import sectery.Http
import sectery.Info
import sectery.Producer
import sectery.Response
import sectery.Rx
import sectery.Tx
import zio.Clock
import zio.RIO
import zio.ZIO

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

    def search(q: String): RIO[Http.Http, List[Search]] =
      implicit val formats: Formats = DefaultFormats
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
          ZIO.attempt {
            parse(body).extract[List[Search]]
          }
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

    case class Frame(
        Id: Long,
        Episode: String,
        Timestamp: Long
    )

    case class Subtitles(
        Id: Long,
        RepresentativeTimestamp: Long,
        Episode: String,
        StartTimestamp: Long,
        EndTimestamp: Long,
        Content: String,
        Language: String
    )

    case class Nearby(
        Id: Long,
        Episode: String,
        Timestamp: Long
    )

    case class Caption(
        Episode: Episode,
        Frame: Frame,
        Subtitles: List[Subtitles],
        Nearby: List[Nearby]
    )

    def caption(e: String, t: Long): RIO[Http.Http, Option[Caption]] =
      implicit val formats: Formats = DefaultFormats
      Http
        .request(
          method = "GET",
          url = s"""https://frinkiac.com/api/caption?e=${e}&t=${t}""",
          headers =
            Map("User-Agent" -> "bot", "Accept" -> "application/json"),
          body = None
        )
        .flatMap { case Response(200, _, body) =>
          ZIO.attempt {
            parse(body).extractOpt[Caption]
          }
        }

  private val frinkiac = """^@frinkiac\s+(.+)\s*$""".r

  override def help(): Iterable[Info] =
    Some(
      Info(
        "@frinkiac",
        "@frinkiac <quote>, e.g. @frinkiac i got to think of a lie fast"
      )
    )

  override def apply(m: Rx): RIO[Http.Http, Iterable[Tx]] =
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
