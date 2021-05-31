package sectery

import java.sql.Connection
import java.sql.DriverManager
import zio.Has
import zio.Task
import zio.ULayer
import zio.ZIO
import zio.ZLayer

object TestFinnhub:
  def apply(): ULayer[Has[Finnhub.Service]] =
    ZLayer.succeed {
      new Finnhub.Service:
        override def quote(symbol: String): Task[Option[Quote]] =
          symbol match
            case "FOO" =>
              ZIO.effectTotal(
                Some(
                  Quote(
                    open = 5,
                    high = 7,
                    low = 4,
                    current = 6,
                    previousClose = 4
                  )
                )
              )
            case _ =>
              ZIO.effectTotal(None)
    }
