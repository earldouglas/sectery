package sectery.producers

import org.slf4j.LoggerFactory
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import scala.collection.JavaConverters._
import sectery.Http
import sectery.Info
import sectery.Producer
import sectery.Response
import sectery.Rx
import sectery.Tx
import zio.clock.Clock
import zio.Has
import zio.URIO
import zio.ZIO

object Html extends Producer:

  private val url = """.*(http[^\s]+).*""".r

  override def help(): Iterable[Info] =
    None

  override def apply(m: Rx): URIO[Http.Http, Iterable[Tx]] =
    m match
      case Rx(c, _, url(url)) =>
        Http.request(
          method = "GET",
          url = url,
          headers = Map("User-Agent" -> "bot"),
          body = None
        ).map {
          case Response(200, _, body) =>
            getDescription(body).map(d => Tx(c, d))
          case r =>
            None
        }.catchAll { e =>
          LoggerFactory.getLogger(this.getClass()).error("caught exception", e)
          ZIO.effectTotal(None)
        }.map(_.toIterable)
      case _ =>
        ZIO.effectTotal(None)

  def getDescription(body: String): Option[String] =

    def nonEmpty(x: String): Option[String] =
      Option(x).map(_.trim).filter(_.length > 0)

    val doc: Document = Jsoup.parse(body)

    val elements: List[Element] =
      doc.select("head").asScala.toList ++
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
