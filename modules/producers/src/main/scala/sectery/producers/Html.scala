package sectery.producers

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import scala.collection.JavaConverters._
import sectery.Http
import sectery.Producer
import sectery.Response
import sectery.Rx
import sectery.Tx
import zio.Clock
import zio.ZIO

object Html extends Producer:

  private val url = """.*(http[^\s]+).*""".r

  override def help(): Iterable[Info] =
    None

  override def apply(m: Rx): ZIO[Http.Http, Throwable, Iterable[Tx]] =
    m match
      case Rx(c, _, url(url)) =>
        Http
          .request(
            method = "GET",
            url = url,
            headers = Map("User-Agent" -> "bot"),
            body = None
          )
          .flatMap {
            case Response(200, _, body) =>
              val doc: Document = Jsoup.parse(body)
              val title: Option[String] = getTitle(doc)
              val desc: Option[String] = getDescription(doc)
              val titleDesc: Option[String] =
                (title, desc) match
                  case (None, None)    => None
                  case (Some(t), None) => Some(t)
                  case (None, Some(d)) => Some(d)
                  case (Some(t), Some(d)) =>
                    Some(List(t, ": ", d).mkString)
              ZIO.attempt(titleDesc.map(m => Tx(c, shorten(m))))
            case r =>
              for
                _ <- ZIO.logError(s"unexpected response: ${r}")
              yield None
          }
      case _ =>
        ZIO.succeed(None)

  private def nonEmpty(x: String): Option[String] =
    Option(x).map(_.trim).filter(_.length > 0)

  private def shorten(x: String): String =
    if x.length > 300 then x.take(280) + "..." else x

  private def getTitle(doc: Document): Option[String] =

    val elements: List[Element] =
      doc.select("title").asScala.toList ++
        doc.select("meta[name=og:title]").asScala.toList ++
        doc.select("meta[property=og:title]").asScala.toList ++
        doc.select("meta[name=twitter:title]").asScala.toList ++
        doc.select("meta[property=twitter:title]").asScala.toList ++
        doc.select("meta[name=title]").asScala.toList ++
        doc.select("meta[property=title]").asScala.toList

    val titles: List[String] =
      elements.map(_.attr("content"))

    (titles :+ doc.title())
      .flatMap(nonEmpty)
      .headOption
      .map(_.replaceAll("[\\r\\n]", " ").replaceAll("\\s+", " "))

  private def getDescription(doc: Document): Option[String] =

    val elements: List[Element] =
      doc.select("meta[name=og:description]").asScala.toList ++
        doc.select("meta[property=og:description]").asScala.toList ++
        doc.select("meta[name=twitter:description]").asScala.toList ++
        doc
          .select("meta[property=twitter:description]")
          .asScala
          .toList ++
        doc.select("meta[name=description]").asScala.toList ++
        doc.select("meta[property=description]").asScala.toList

    val descriptions: List[String] =
      elements.map(_.attr("content"))

    descriptions
      .flatMap(nonEmpty)
      .headOption
      .map(_.replaceAll("[\\r\\n]", " ").replaceAll("\\s+", " "))
