package sectery.producers

import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import sectery.Db
import sectery.Producer
import sectery.Rx
import sectery.Tx
import zio.Clock
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

  private def getTime(zone: String): ZIO[Clock, Nothing, String] =
    for
      millis <- Clock.currentTime(TimeUnit.MILLISECONDS)
      date = new Date(millis)
      sdf = new SimpleDateFormat("EEE, d MMM yyyy, kk:mm zzz")
      _ = sdf.setTimeZone(TimeZone.getTimeZone(zone))
      pretty = sdf.format(date)
    yield pretty

  override def apply(
      m: Rx
  ): ZIO[Db.Db with Clock, Throwable, Iterable[Tx]] =
    m match
      case Rx(c, nick, "@time") =>
        for
          zo <- Config.getConfig(nick, "tz")
          t <- zo match
            case Some(z) =>
              getTime(z).map(t => Some(Tx(c, t)))
            case None =>
              ZIO.succeed(
                Some(
                  Tx(
                    c,
                    s"${nick}: Set default time zone with `@set tz <zone>`"
                  )
                )
              )
        yield t
      case Rx(c, _, time(_, zone)) =>
        getTime(zone).map(t => Some(Tx(c, t)))
      case _ =>
        ZIO.succeed(None)
