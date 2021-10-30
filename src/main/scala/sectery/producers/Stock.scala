package sectery.producers

import sectery.Db
import sectery.Finnhub
import sectery.Http
import sectery.Info
import sectery.Producer
import sectery.Response
import sectery.Rx
import sectery.Tx
import zio.Clock
import zio.Has
import zio.RIO
import zio.Task
import zio.UIO
import zio.ZIO

class Stock(finnhubApiToken: String) extends Producer:

  private val finnhub: Finnhub =
    new Finnhub(finnhubApiToken)

  private val stock = """^@stock\s+([^\s]+([\s,]+([^\s]+))*)$""".r

  override def help(): Iterable[Info] =
    Some(
      Info(
        "@stock",
        "@stock [symbol1] [symbol2] ... [symbolN], e.g. @stock GME"
      )
    )

  private def getStock(
      symbolsString: String
  ): RIO[Http.Http, Iterable[String]] =

    val symbols: Set[String] =
      symbolsString
        .split("""[\s,]+""")
        .map(_.trim())
        .map(_.toUpperCase())
        .toSet

    val getSymbols: Set[RIO[Http.Http, Option[String]]] =
      symbols.map { symbol =>
        finnhub
          .quote(symbol.toUpperCase())
          .map {
            case Some(q) =>
              val current = q.current
              val change = q.current - q.previousClose
              val changeP = change * 100 / q.previousClose
              Some(
                f"""${symbol}: ${current}%.2f ${change}%+.2f (${changeP}%+.2f%%)"""
              )
            case None =>
              Some(s"${symbol}: stonk not found")
          }
      }

    ZIO.collectAll(getSymbols).map(_.flatten)

  override def apply(m: Rx): RIO[Db.Db with Http.Http, Iterable[Tx]] =
    m match
      case Rx(c, _, stock(symbolsString, _, _)) =>
        getStock(symbolsString).map(
          _.map(m => Tx(channel = c, message = m))
        )

      case Rx(c, nick, "@stock") =>
        for
          so <- Config.getConfig(nick, "stock")
          txs <- so match
            case Some(symbolsString) =>
              val txs: RIO[Db.Db with Http.Http, Iterable[Tx]] =
                getStock(symbolsString).map(
                  _.map(m => Tx(channel = c, message = m))
                )
              txs
            case None =>
              val txs: RIO[Db.Db with Http.Http, Iterable[Tx]] =
                ZIO.succeed(
                  Some(
                    Tx(
                      c,
                      "Set default symbols with `@set stock <symbol1> [symbol2] ... [symbolN]`"
                    )
                  )
                )
              txs
        yield txs
      case _ =>
        ZIO.succeed(None)
