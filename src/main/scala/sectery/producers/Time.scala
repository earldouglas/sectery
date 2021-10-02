package sectery.producers

import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import sectery.Info
import sectery.Producer
import sectery.Rx
import sectery.Tx
import zio.Clock
import zio.Has
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

  private def getTime(zone: String): URIO[Has[Clock], String] =
    for
      millis <- Clock.currentTime(TimeUnit.MILLISECONDS)
      date = new Date(millis)
      sdf = new SimpleDateFormat("EEE, d MMM yyyy, kk:mm zzz")
      _ = sdf.setTimeZone(TimeZone.getTimeZone(zone))
      pretty = sdf.format(date)
    yield pretty

  override def apply(m: Rx): URIO[Has[Clock], Iterable[Tx]] =
    m match
      case Rx(c, _, "@time") =>
        getTime("America/Phoenix").map(t => Some(Tx(c, t)))
      case Rx(c, _, time(_, zone)) =>
        getTime(zone).map(t => Some(Tx(c, t)))
      case _ =>
        ZIO.succeed(None)
