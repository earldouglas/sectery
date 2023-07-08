package sectery.producers

import sectery.Db
import sectery.Http
import sectery.Producer
import sectery.Rx
import sectery.Tx
import zio.ZIO

object Blink extends Producer:

  private val blink = """^@blink\s(.+)$""".r

  override def help(): Iterable[Info] =
    Some(Info("@blink", "@blink <text>"))

  override def apply(
      m: Rx
  ): ZIO[Db.Db with Http.Http, Throwable, Iterable[Tx]] =
    m match
      case Rx(c, _, blink(text)) =>
        val b = '\u0006'
        ZIO.succeed(Some(Tx(c, s"${b}${text}${b}")))
      case _ =>
        ZIO.succeed(None)
