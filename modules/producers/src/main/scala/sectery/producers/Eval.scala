package sectery.producers

import java.net.URLEncoder
import sectery.Http
import sectery.Producer
import sectery.Response
import sectery.Rx
import sectery.Tx
import zio.ZIO

object Eval extends Producer:

  private val eval = """^@eval\s+(.+)\s*$""".r

  override def help(): Iterable[Info] =
    Some(Info("@eval", "@eval <expression>, e.g. @eval 6 * 7"))

  override def apply(m: Rx): ZIO[Http.Http, Throwable, Iterable[Tx]] =
    m match
      case Rx(c, _, eval(expr)) =>
        val encExpr = URLEncoder.encode(expr, "UTF-8")
        Http
          .request(
            method = "GET",
            url =
              s"https://earldouglas.com/api/haskeval?src=${encExpr}",
            headers = Map.empty,
            body = None
          )
          .flatMap {
            case Response(200, _, body) =>
              ZIO.succeed(Some(Tx(c, body)))
            case r =>
              for _ <- ZIO.logError(s"unexpected response: ${r}")
              yield None
          }
      case _ =>
        ZIO.succeed(None)
