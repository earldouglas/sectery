package sectery.producers

import java.net.URLEncoder
import org.slf4j.LoggerFactory
import sectery.Http
import sectery.Info
import sectery.Producer
import sectery.Response
import sectery.Rx
import sectery.Tx
import zio.Clock
import zio.Has
import zio.RIO
import zio.ZIO

object Eval extends Producer:

  private val eval = """^@eval\s+(.+)\s*$""".r

  override def help(): Iterable[Info] =
    Some(Info("@eval", "@eval <expression>, e.g. @eval 6 * 7"))

  override def apply(m: Rx): RIO[Http.Http, Iterable[Tx]] =
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
          .map {
            case Response(200, _, body) =>
              Some(Tx(c, body))
            case r =>
              LoggerFactory
                .getLogger(this.getClass())
                .error(s"unexpected response: ${r}")
              None
          }
      case _ =>
        ZIO.succeed(None)
