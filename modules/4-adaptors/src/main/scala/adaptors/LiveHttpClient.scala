package sectery.adaptors

import java.net.HttpURLConnection
import java.net.URL
import scala.io.Source
import scala.jdk.CollectionConverters._
import sectery._
import sectery.control.Monad
import sectery.effects.HttpClient.Response
import sectery.effects._

object LiveHttpClient:

  def unsafeRequest(
      method: String,
      url: URL,
      headers: Map[String, String],
      body: Option[String]
  ): Response =
    val c =
      url
        .openConnection()
        .asInstanceOf[HttpURLConnection]

    c.setInstanceFollowRedirects(true)
    c.setRequestMethod(method)
    c.setDoInput(true)
    c.setDoOutput(body.isDefined)

    headers foreach { case (k, v) =>
      c.setRequestProperty(k, v)
    }

    body foreach { b =>
      c.getOutputStream.write(b.getBytes("UTF-8"))
    }

    val response =
      Response(
        status = c.getResponseCode(),
        headers = c
          .getHeaderFields()
          .asScala
          .filter({ case (k, _) => k != null })
          .map({ case (k, v) => (k, v.asScala.mkString(",")) })
          .toMap - "Date" - "Content-Length" - "Server",
        body = Source.fromInputStream {
          if c.getResponseCode() < 400 then c.getInputStream
          else c.getErrorStream
        }.mkString
      )

    c.disconnect()

    response

class LiveHttpClient[F[_]: Monad] extends HttpClient[F]:

  override def request(
      method: String,
      url: URL,
      headers: Map[String, String],
      body: Option[String]
  ) =
    summon[Monad[F]]
      .pure(
        LiveHttpClient
          .unsafeRequest(
            method = method,
            url = url,
            headers = headers,
            body = body
          )
      )
