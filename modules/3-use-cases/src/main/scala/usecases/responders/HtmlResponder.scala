package sectery.usecases.responders

import java.net.URI
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import scala.jdk.CollectionConverters._
import sectery.control.Monad
import sectery.control.Monad._
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases.Responder

class HtmlResponder[F[_]: Monad: HttpClient] extends Responder[F]:

  override def name = "http[s]"

  override def usage = "https://example.com/"

  private val url = """.*(http[^\s]+).*""".r

  override def respondToMessage(rx: Rx) =
    rx.message match
      case url(url) =>
        summon[HttpClient[F]]
          .request("GET", new URI(url).toURL(), Map.empty, None)
          .map { response =>
            val doc: Document = Jsoup.parse(response.body)
            val title: Option[String] = getTitle(doc)
            val desc: Option[String] = getDescription(doc)
            val titleDesc: Option[String] =
              (title, desc) match
                case (None, None)    => None
                case (Some(t), None) => Some(t)
                case (None, Some(d)) => Some(d)
                case (Some(t), Some(d)) =>
                  Some(List(t, ": ", d).mkString)
            titleDesc
              .map(m =>
                Tx(
                  service = rx.service,
                  channel = rx.channel,
                  thread = rx.thread,
                  message = shorten(m)
                )
              )
              .toList
          }
      case _ =>
        summon[Monad[F]].pure(Nil)

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
