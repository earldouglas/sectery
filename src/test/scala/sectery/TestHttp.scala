package sectery

import zio._

object TestHttp:

  def apply(): ULayer[Has[Http.Service]] =
    apply(
      resStatus = 200,
      resHeaders = Map.empty,
      resBody = ""
    )

  def apply(
      resStatus: Int,
      resHeaders: Map[String, String],
      resBody: String
  ): ULayer[Has[Http.Service]] =
    ZLayer.succeed {
      new Http.Service:
        def request(
            method: String,
            url: String,
            headers: Map[String, String],
            body: Option[String]
        ): UIO[Response] =
          ZIO.effectTotal {
            Response(
              status = resStatus,
              headers = resHeaders,
              body = resBody
            )
          }
    }
