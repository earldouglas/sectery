package sectery.producers

import sectery.Producer

object FinnhubApi:

  import zio.json.DeriveJsonDecoder
  import zio.json.JsonDecoder

  case class Quote(
      c: Float, // current
      h: Float, // high
      l: Float, // low
      o: Float, // open
      pc: Float, // previous close
      t: Long // time
  )

  object Quote:
    implicit val decoder: JsonDecoder[Quote] =
      DeriveJsonDecoder.gen[Quote]

class FinnhubClient(apiToken: String):

  import sectery.Response
  import sectery.Http
  import zio.ZIO
  import zio.json._

  case class Quote(
      open: Float,
      high: Float,
      low: Float,
      current: Float,
      previousClose: Float
  )

  def quote(symbol: String): ZIO[Http.Http, Throwable, Option[Quote]] =
    Http
      .request(
        method = "GET",
        url =
          s"""https://finnhub.io/api/v1/quote?symbol=${symbol}&token=${apiToken}""",
        headers = Map("User-Agent" -> "bot"),
        body = None
      )
      .flatMap { case Response(200, _, body) =>
        body.fromJson[FinnhubApi.Quote] match
          case Right(jq)
              if jq.c != 0 || jq.h != 0 || jq.l != 0 || jq.o != 0 || jq.pc != 0 =>
            ZIO.succeed(
              Some(
                Quote(
                  open = jq.o.toFloat,
                  high = jq.h.toFloat,
                  low = jq.l.toFloat,
                  current = jq.c.toFloat,
                  previousClose = jq.pc.toFloat
                )
              )
            )
          case Right(_) => ZIO.succeed(None)
          case Left(_)  => ZIO.succeed(None)
      }

class Stock(finnhubApiToken: String) extends Producer:

  import sectery.Db
  import sectery.Http
  import sectery.Rx
  import sectery.Tx
  import zio.ZIO

  private val finnhub: FinnhubClient =
    new FinnhubClient(finnhubApiToken)

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
  ): ZIO[Http.Http, Throwable, Iterable[String]] =

    val symbols: Set[String] =
      symbolsString
        .split("""[\s,]+""")
        .map(_.trim())
        .map(_.toUpperCase())
        .toSet

    val getSymbols: Set[ZIO[Http.Http, Throwable, Option[String]]] =
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

  override def apply(
      m: Rx
  ): ZIO[Db.Db with Http.Http, Throwable, Iterable[Tx]] =
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
              val txs
                  : ZIO[Db.Db with Http.Http, Throwable, Iterable[Tx]] =
                getStock(symbolsString).map(
                  _.map(m => Tx(channel = c, message = m))
                )
              txs
            case None =>
              val txs
                  : ZIO[Db.Db with Http.Http, Throwable, Iterable[Tx]] =
                ZIO.succeed(
                  Some(
                    Tx(
                      c,
                      s"${nick}: Set default symbols with `@set stock <symbol1> [symbol2] ... [symbolN]`"
                    )
                  )
                )
              txs
        yield txs
      case _ =>
        ZIO.succeed(None)
