package sectery.producers

import org.slf4j.LoggerFactory
import sectery.Finnhub
import sectery.Http
import sectery.Info
import sectery.Producer
import sectery.Response
import sectery.Rx
import sectery.Tx
import zio.Clock
import zio.Has
import zio.Task
import zio.UIO
import zio.URIO
import zio.ZIO

class Stock(finnhubApiToken: String) extends Producer:

  private val finnhub: Finnhub =
    new Finnhub(finnhubApiToken)

  private val stock = """^@stock\s+([^\s]+)\s*$""".r

  override def help(): Iterable[Info] =
    Some(Info("@stock", "@stock <symbol>, e.g. @stock GME"))

  override def apply(m: Rx): URIO[Http.Http, Iterable[Tx]] =
    m match
      case Rx(c, _, stock(symbol)) =>
        finnhub
          .quote(symbol.toUpperCase())
          .map {
            case Some(q) =>
              val current = q.current
              val change = q.current - q.previousClose
              val changeP = change * 100 / q.previousClose
              Some(
                Tx(
                  c,
                  f"""${symbol}: ${current}%.2f ${change}%+.2f (${changeP}%+.2f%%)"""
                )
              )
            case None =>
              Some(Tx(c, s"${symbol}: stonk not found"))
          }
          .map(_.toIterable)
      case _ =>
        ZIO.succeed(None)
