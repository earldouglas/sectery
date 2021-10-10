package sectery.producers

import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.Date
import java.util.TimeZone
import sectery.Db
import sectery.Info
import sectery.Producer
import sectery.Rx
import sectery.Tx
import zio.Clock
import zio.Has
import zio.RIO
import zio.URIO
import zio.ZIO

object Time extends Producer:

  private val time = """^@time(\s+([^\s]+)\s*)?$""".r

  override def help(): Iterable[Info] =
    Some(
      Info(
        "@time",
        "@time [zone], e.g. @time, @time MST, @time GMT-7, @time America/Phoenix"
      )
    )

  private def getTz(nick: String): RIO[Db.Db, Option[String]] =
    Config.getConfig(nick, "tz")

  private def getTime(zone: String): URIO[Has[Clock], String] =
    for
      millis <- Clock.currentTime(TimeUnit.MILLISECONDS)
      date = new Date(millis)
      sdf = new SimpleDateFormat("EEE, d MMM yyyy, kk:mm zzz")
      _ = sdf.setTimeZone(TimeZone.getTimeZone(zone))
      pretty = sdf.format(date)
    yield pretty

  override def apply(m: Rx): RIO[Db.Db with Has[Clock], Iterable[Tx]] =
    m match
      case Rx(c, nick, "@time") =>
        for
          zo <- getTz(nick)
          t <- zo match
            case Some(z) =>
              getTime(z).map(t => Some(Tx(c, t)))
            case None =>
              getTime("America/Phoenix").map(t => Some(Tx(c, t)))
        yield t
      case Rx(c, _, time(_, zone)) =>
        getTime(zone).map(t => Some(Tx(c, t)))
      case _ =>
        ZIO.succeed(None)
