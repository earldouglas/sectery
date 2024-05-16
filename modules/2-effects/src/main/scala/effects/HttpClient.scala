package sectery.effects

import java.net.URL

trait HttpClient[F[_]]:
  def request(
      method: String,
      url: URL,
      headers: Map[String, String],
      body: Option[String]
  ): F[HttpClient.Response]

object HttpClient:
  case class Response(
      status: Int,
      headers: Map[String, String],
      body: String
  )
