package sectery

import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.TimeZone

object PrettyTime:

  private def apply(
      instant: Instant,
      timeZone: Option[TimeZone]
  ): String =
    val sdf = new SimpleDateFormat("EEE, d MMM yyyy, kk:mm zzz")
    timeZone match
      case Some(tz) =>
        sdf.setTimeZone(tz)
        sdf.format(Date.from(instant))
      case None =>
        sdf.format(Date.from(instant))

  def apply(instant: Instant, timeZoneString: String): String =
    apply(instant, Some(TimeZone.getTimeZone(timeZoneString)))

  def apply(instant: Instant): String =
    apply(instant, None)
