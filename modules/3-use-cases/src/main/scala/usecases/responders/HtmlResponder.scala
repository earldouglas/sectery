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

  def shorten(x: String): String =
    if x.length > 300 then x.take(280) + "..." else x

  def clean(x: String): String =
    x
      .replaceAll("[\\r\\n]", " ")
      .replaceAll("\\s+", " ")

  def select(doc: Document, selector: String): List[Element] =
    doc
      .select(selector)
      .asScala
      .toList

  def selectText(doc: Document, selector: String): List[String] =
    select(doc, selector)
      .map(_.text())
      .flatMap(nonEmpty)
      .map(clean(_))

  def selectContentAttribute(
      doc: Document,
      selector: String
  ): List[String] =
    select(doc, selector)
      .map(_.attr("content"))
      .flatMap(nonEmpty)
      .map(clean(_))

  def getTitle(doc: Document): List[String] =
    selectText(doc, "title")

  def getMeta(doc: Document, key: String): List[String] =
    List("", "og:", "twitter:")
      .flatMap { prefix =>
        selectContentAttribute(doc, s"meta[name=${key}]") ++
          selectContentAttribute(doc, s"meta[property=${key}]")
      }

  def getDescription(doc: Document): List[String] =
    getMeta(doc, "description")

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

            List(
              HtmlResponder.getTitle(doc).headOption,
              HtmlResponder.getDescription(doc).headOption
            )
              .flatMap { xs =>
                xs.map { x =>
                  Tx(
                    service = rx.service,
                    channel = rx.channel,
                    thread = rx.thread,
                    message = HtmlResponder.shorten(x)
                  )
                }
              }
          }
      case _ =>
        summon[Monad[F]].pure(Nil)
