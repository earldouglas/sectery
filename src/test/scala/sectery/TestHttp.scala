package sectery

import zio._

object TestHttp:

  def apply(): ULayer[Http.Service] =
    apply(
      resStatus = 200,
      resHeaders = Map.empty,
      resBody = ""
    )

  def apply(
      resStatus: Int,
      resHeaders: Map[String, String],
      resBody: String
  ): ULayer[Http.Service] =
    ZLayer.succeed[Http.Service] {
      new Http.Service:
        def request(
            method: String,
            url: String,
            headers: Map[String, String],
            body: Option[String]
        ): UIO[Response] =
          ZIO.succeed {
            Response(
              status = resStatus,
              headers = resHeaders,
              body = resBody
            )
          }
    }
