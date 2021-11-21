package sectery

import scala.util.Try
import sectery.Http
import zio.RIO
import zio.Task
import zio.ULayer
import zio.ZIO
import zio.ZLayer
import zio.json._

object FinnhubApi:

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

class Finnhub(apiToken: String):

  case class Quote(
      open: Float,
      high: Float,
      low: Float,
      current: Float,
      previousClose: Float
  )

  def quote(symbol: String): RIO[Http.Http, Option[Quote]] =
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
