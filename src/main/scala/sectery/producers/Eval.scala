package sectery.producers

import org.slf4j.LoggerFactory
import java.net.URLEncoder
import sectery.Http
import sectery.Producer
import sectery.Response
import sectery.Rx
import sectery.Tx
import zio.clock.Clock
import zio.Has
import zio.URIO
import zio.ZIO

object Eval extends Producer:
  private val eval = """^@eval\s+(.+)\s*$""".r
  override def apply(m: Rx): URIO[Http.Http, Iterable[Tx]] =
    m match
      case Rx(c, _, eval(expr)) =>
        val encExpr = URLEncoder.encode(expr, "UTF-8")
        Http.request(
          method = "GET",
          url = s"https://earldouglas.com/api/haskeval?src=${encExpr}",
          headers = Map.empty,
          body = None
        ).map {
          case Response(200, _, body) =>
            Some(Tx(c, body))
          case r =>
            None
        }.catchAll { e =>
          LoggerFactory.getLogger(this.getClass()).error("caught exception", e)
          ZIO.effectTotal(None)
        }.map(_.toIterable)
      case _ =>
        ZIO.effectTotal(None)
