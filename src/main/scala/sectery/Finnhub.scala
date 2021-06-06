package sectery

import org.slf4j.LoggerFactory
import zio.Has
import zio.RIO
import zio.Task
import zio.ULayer
import zio.ZIO
import zio.ZLayer

case class Quote(
    open: Float,
    high: Float,
    low: Float,
    current: Float,
    previousClose: Float
)

object Finnhub:

  type Finnhub = Has[Finnhub.Service]

  trait Service:
    def quote(symbol: String): Task[Option[Quote]]

  val live: ULayer[Has[Service]] =
    ZLayer.succeed {
      new Service:
        private lazy val token: String = sys.env("FINNHUB_API_TOKEN")
        def quote(symbol: String): Task[Option[Quote]] =
          ZIO
            .effect {
              Http.unsafeRequest(
                method = "GET",
                url =
                  s"""https://finnhub.io/api/v1/quote?symbol=${symbol}&token=${token}""",
                headers = Map.empty,
                body = None
              ) match
                case Response(200, _, body) =>
                  // TODO this is awful, but I can't find a JSON parser that supports Scala 3
                  val fields: Map[String, String] =
                    body
                      .replaceAll("""["{}\s\n\r]""", "")
                      .split(",")
                      .map(_.split(":"))
                      .map(xs => xs(0) -> xs(1))
                      .toMap
                  for {
                    o <- fields.get("o").map(_.toFloat)
                    h <- fields.get("h").map(_.toFloat)
                    l <- fields.get("l").map(_.toFloat)
                    c <- fields.get("c").map(_.toFloat)
                    pc <- fields.get("pc").map(_.toFloat)
                    q <- (o, h, l, c, pc) match
                      case (0, 0, 0, 0, 0) =>
                        None
                      case _ =>
                        Some(
                          Quote(
                            open = o,
                            high = h,
                            low = l,
                            current = c,
                            previousClose = pc
                          )
                        )
                  } yield q
                case _ =>
                  None
            }
            .catchAll { e =>
              LoggerFactory
                .getLogger(this.getClass())
                .error("caught exception", e)
              ZIO.effectTotal(None)
            }
    }

  def quote(symbol: String): RIO[Finnhub, Option[Quote]] =
    ZIO.accessM(_.get.quote(symbol))
