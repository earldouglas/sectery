package sectery.producers

import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import scala.collection.JavaConverters._
import sectery.Http
import sectery.Info
import sectery.Producer
import sectery.Response
import sectery.Rx
import sectery.Tx
import zio.UIO
import zio.URIO
import zio.ZIO

object Zillow extends Producer:

  private val zillow = """^@zillow\s+(.+)\s*$""".r

  override def help(): Iterable[Info] =
    Some(
      Info(
        "@zillow",
        "@zillow <address>, e.g. @zillow 123 main st, smithville, tx"
      )
    )

  override def apply(m: Rx): URIO[Http.Http, Iterable[Tx]] =
    m match
      case Rx(c, _, zillow(address)) =>
        val addressEnc =
          address
            .replaceAll("[^a-zA-Z0-9]", " ")
            .trim()
            .replaceAll("\\s+", "-")
        val url = s"https://www.zillow.com/homes/${addressEnc}_rb/"
        Http
          .request(
            method = "GET",
            url = url,
            headers = Map(
              "User-Agent" -> "Mozilla/5.0 (X11; Linux x86_64)"
            ),
            body = None
          )
          .map {
            case Response(200, _, body) =>
              Jsoup
                .parse(body)
                .select("input[id=Est.-selling-price-of-your-home]")
                .asScala
                .headOption
                .map(_.attr("value"))
                .map(value => Tx(c, s"Zestimate: $$${value}"))
            case r =>
              LoggerFactory
                .getLogger(this.getClass())
                .error("unexpected response", r)
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
