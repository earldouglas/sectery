package sectery

import java.net.HttpURLConnection
import java.net.URL
import scala.collection.JavaConverters._
import scala.io.Source
import zio.Has
import zio.RIO
import zio.Task
import zio.ULayer
import zio.ZIO
import zio.ZLayer

case class Response(
    status: Int,
    headers: Map[String, String],
    body: String
)

object Http:

  type Http = Has[Http.Service]

  trait Service:
    def request(
        method: String,
        url: String,
        headers: Map[String, String],
        body: Option[String]
    ): Task[Response]

  val live: ULayer[Has[Service]] =
    ZLayer.succeed {
      new Service:
        def request(
            method: String,
            url: String,
            headers: Map[String, String],
            body: Option[String]
        ): Task[Response] =
          ZIO.attempt {
            Http.unsafeRequest(
              method = method,
              url = url,
              headers = headers,
              body = body
            )
          }
    }

  def request(
      method: String,
      url: String,
      headers: Map[String, String],
      body: Option[String]
  ): RIO[Http, Response] =
    ZIO.accessZIO(_.get.request(method, url, headers, body))

  def unsafeRequest(
      method: String,
      url: String,
      headers: Map[String, String],
      body: Option[String]
  ): Response =
    val c =
      new URL(url)
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
