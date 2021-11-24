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
import zio.RIO
import zio.Task
import zio.UIO
import zio.ZIO

object Blink extends Producer:

  private val blink = """^@blink\s(.+)$""".r

  override def help(): Iterable[Info] =
    Some(Info("@blink", "@blink <text>"))

  override def apply(m: Rx): RIO[Db.Db with Http.Http, Iterable[Tx]] =
    m match
      case Rx(c, _, blink(text)) =>
        val b = '\u0006'
        ZIO.succeed(Some(Tx(c, s"${b}${text}${b}")))
      case _ =>
        ZIO.succeed(None)
