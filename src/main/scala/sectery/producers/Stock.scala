package sectery.producers

import org.slf4j.LoggerFactory
import sectery.Finnhub
import sectery.Producer
import sectery.Response
import sectery.Rx
import sectery.Tx
import zio.clock.Clock
import zio.Has
import zio.URIO
import zio.UIO
import zio.Task
import zio.ZIO

object Stock extends Producer:
  private val stock = """^@stock\s+([^\s]+)\s*$""".r
  override def apply(m: Rx): URIO[Finnhub.Finnhub, Iterable[Tx]] =
    m match
      case Rx(c, _, stock(symbol)) =>
        Finnhub
          .quote(symbol.toUpperCase())
          .map {
            case Some(q) =>
              val current = q.current
              val change = q.current - q.previousClose
              val changeP = change * 100 / q.previousClose
              Some(Tx(c, f"""${symbol}: ${current}%.2f ${change}%+.2f (${changeP}%+.2f%%)"""))
            case None =>
              Some(Tx(c, s"${symbol}: stonk not found"))
          }.catchAll { e =>
            LoggerFactory.getLogger(this.getClass()).error("caught exception", e)
            ZIO.effectTotal(None)
          }.map(_.toIterable)
      case _ =>
        ZIO.effectTotal(None)
