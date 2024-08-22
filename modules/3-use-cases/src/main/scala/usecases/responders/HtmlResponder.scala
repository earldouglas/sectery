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

object HtmlResponder:

  private def nonEmpty(x: String): Option[String] =
    Option(x).map(_.trim).filter(_.length > 0)

  val url = """.*(http[^\s]+).*""".r

  private def shorten(x: String): String =
    if x.length > 300 then x.take(280) + "..." else x

  private def clean(x: String): String =
    x
      .replaceAll("[\\r\\n]", " ")
      .replaceAll("\\s+", " ")

  private def select(doc: Document, selector: String): List[Element] =
    doc
      .select(selector)
      .asScala
      .toList

  private def selectText(
      doc: Document,
      selector: String
  ): List[String] =
    select(doc, selector)
      .map(_.text())

  private def selectContentAttribute(
      doc: Document,
      selector: String
  ): List[String] =
    select(doc, selector)
      .map(_.attr("content"))
      .flatMap(nonEmpty)
      .map(clean(_))

  private def getMeta(doc: Document, key: String): List[String] =
    List("", "og:", "twitter:")
      .flatMap { prefix =>
        selectContentAttribute(doc, s"meta[name=${key}]") ++
          selectContentAttribute(doc, s"meta[property=${key}]")
      }

  def getTitle(doc: Document): Option[String] =
    selectText(doc, "title")
      .flatMap(nonEmpty)
      .map(clean(_))
      .map(shorten)
      .headOption

  def getDescription(doc: Document): Option[String] =
    getMeta(doc, "description")
      .flatMap(nonEmpty)
      .map(clean(_))
      .map(shorten)
      .headOption

class HtmlResponder[F[_]: Monad: HttpClient] extends Responder[F]:

  override def name = "http[s]"

  override def usage = "https://example.com/"

  override def respondToMessage(rx: Rx) =
    rx.message match
      case HtmlResponder.url(url) =>
        summon[HttpClient[F]]
          .request("GET", new URI(url).toURL(), Map.empty, None)
          .map { response =>

            val doc: Document = Jsoup.parse(response.body)

            (
              HtmlResponder.getTitle(doc),
              HtmlResponder.getDescription(doc)
            ) match
              case (Some("- YouTube"), _) =>
                Nil
              case (Some("Blocked"), _) =>
                Nil
              case (t, d) =>
                List(t, d).flatten
                  .map { x =>
                    Tx(
                      service = rx.service,
                      channel = rx.channel,
                      thread = rx.thread,
                      message = x
                    )
                  }
          }
      case _ =>
        summon[Monad[F]].pure(Nil)
