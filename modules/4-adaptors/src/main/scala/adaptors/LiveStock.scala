package sectery.adaptors

import java.net.URI
import sectery._
import sectery.control.Monad
import sectery.control.Monad._
import sectery.effects.HttpClient.Response
import sectery.effects._
import zio.json._

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

object LiveStock:
  def apply[F[_]: HttpClient: Monad](
      finnhubApiToken: String
  ): Stock[F] =
    new Stock:

      case class Quote(
          open: Float,
          high: Float,
          low: Float,
          current: Float,
          previousClose: Float
      )

      override def getQuote(symbol: String): F[Option[String]] =
        summon[HttpClient[F]]
          .request(
            method = "GET",
            url = new URI(
              s"""https://finnhub.io/api/v1/quote?symbol=${symbol}&token=${finnhubApiToken}"""
            ).toURL(),
            headers = Map("User-Agent" -> "bot"),
            body = None
          )
          .map { case Response(200, _, body) =>
            body
              .fromJson[FinnhubApi.Quote] match
              case Right(jq)
                  if jq.c != 0 || jq.h != 0 || jq.l != 0 || jq.o != 0 || jq.pc != 0 =>
                val current: Float = jq.c
                val previousClose: Float = jq.pc

                val change: Float = current - previousClose
                val changeP: Float = change * 100 / previousClose

                Some(
                  f"""${symbol}: ${current}%.2f ${change}%+.2f (${changeP}%+.2f%%)"""
                )
              case _ => None
          }
