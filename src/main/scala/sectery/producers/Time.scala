package sectery.producers

import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.Date
import java.util.TimeZone
import sectery.Info
import sectery.Producer
import sectery.Rx
import sectery.Tx
import zio.clock.Clock
import zio.URIO
import zio.ZIO

object Time extends Producer:

  override def help(): Iterable[Info] =
    Some(Info("@time", "@time"))

  override def apply(m: Rx): URIO[Clock, Iterable[Tx]] =
    m match
      case Rx(c, _, "@time") =>
        for
          millis <- zio.clock.currentTime(TimeUnit.MILLISECONDS)
          date = new Date(millis)
          sdf = new SimpleDateFormat("EEE, d MMM yyyy, kk:mm zzz")
          _ = sdf.setTimeZone(TimeZone.getTimeZone("America/Phoenix"))
          pretty = sdf.format(date)
        yield Some(Tx(c, pretty))
      case _ =>
        ZIO.effectTotal(None)
