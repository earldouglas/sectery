package sectery.producers

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
import zio.Has
import zio.URIO
import zio.ZIO
import zio.clock.Clock

object Html extends Producer:

  private val url = """.*(http[^\s]+).*""".r

  override def help(): Iterable[Info] =
    None

  override def apply(m: Rx): URIO[Http.Http, Iterable[Tx]] =
    m match
      case Rx(c, _, url(url)) =>
        Http
          .request(
            method = "GET",
            url = url,
            headers = Map("User-Agent" -> "bot"),
            body = None
          )
          .map {
            case Response(200, _, body) =>
              val doc: Document = Jsoup.parse(body)
              getTitle(doc).map(d => Tx(c, d))
            case r =>
              LoggerFactory
                .getLogger(this.getClass())
                .error(s"unexpected response: ${r}")
              None
          }
          .catchAll { e =>
            LoggerFactory
              .getLogger(this.getClass())
              .error("caught exception", e)
            ZIO.effectTotal(None)
          }
          .map(_.toIterable)
      case _ =>
        ZIO.effectTotal(None)

  def getTitle(doc: Document): Option[String] =

    def nonEmpty(x: String): Option[String] =
      Option(x).map(_.trim).filter(_.length > 0)

    val elements: List[Element] =
      doc.select("title").asScala.toList ++
        doc.select("meta[name=title]").asScala.toList ++
        doc.select("meta[property=title]").asScala.toList ++
        doc.select("meta[name=og:title]").asScala.toList ++
        doc.select("meta[property=og:title]").asScala.toList ++
        doc.select("meta[name=twitter:title]").asScala.toList ++
        doc.select("meta[property=twitter:title]").asScala.toList

    val descriptions: List[String] =
      elements.map(_.attr("content"))

    (descriptions :+ doc.title())
      .flatMap(nonEmpty)
      .map(_.replaceAll("[\\r\\n]", " ").replaceAll("\\s+", " "))
      .headOption

  def getDescription(doc: Document): Option[String] =

    def nonEmpty(x: String): Option[String] =
      Option(x).map(_.trim).filter(_.length > 0)

    val elements: List[Element] =
      doc.select("meta[name=description]").asScala.toList ++
        doc.select("meta[property=description]").asScala.toList ++
        doc.select("meta[name=og:description]").asScala.toList ++
        doc.select("meta[property=og:description]").asScala.toList ++
        doc.select("meta[name=twitter:description]").asScala.toList ++
        doc.select("meta[property=twitter:description]").asScala.toList

    val descriptions: List[String] =
      elements.map(_.attr("content"))

    (descriptions :+ doc.title())
      .flatMap(nonEmpty)
      .map(_.replaceAll("[\\r\\n]", " ").replaceAll("\\s+", " "))
      .headOption
