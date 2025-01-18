package sectery.adaptors

import java.net.URI
import java.net.URLEncoder
import sectery._
import sectery.control.Monad
import sectery.control.Monad._
import sectery.effects.HttpClient.Response
import sectery.effects._

object LiveEval:
  def apply[F[_]: HttpClient: Monad](): Eval[F] =
    new Eval:
      override def eval(src: String): F[Either[String, String]] =
        val encExpr = URLEncoder.encode(src, "UTF-8")
        summon[HttpClient[F]]
          .request(
            method = "GET",
            url = new URI(
              s"https://haskeval.earldouglas.com/?src=${encExpr}"
            ).toURL(),
            headers = Map.empty,
            body = None
          )
          .map {
            case Response(200, _, body) =>
              Right(body)
            case r =>
              Left(s"unexpected response: ${r}")
          }
