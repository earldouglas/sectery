package sectery.producers

import sectery._
import zio._
import zio.test.TestClock

object TimeSpec extends ProducerSpec:

  override val pre = Some(TestClock.setTime(1234567890.millis))

  override val specs =
    Map(
      "@time produces time in user-configured tz" ->
        (
          List(
            Rx("#foo", "bar", "@time"),
            Rx("#foo", "bar", "@set tz PST"),
            Rx("#foo", "bar", "@time")
          ),
          List(
            Tx("#foo", "bar: Set default time zone with @set tz <zone>"),
            Tx("#foo", "bar: tz set to PST"),
            Tx("#foo", "Wed, 14 Jan 1970, 22:56 PST")
          )
        ),
      "@time PST produces time in PST" ->
        (
          List(
            Rx("#foo", "bar", "@time PST"),
            Rx("#foo", "bar", "@set tz EST"),
            Rx("#foo", "bar", "@time PST")
          ),
          List(
            Tx("#foo", "Wed, 14 Jan 1970, 22:56 PST"),
            Tx("#foo", "bar: tz set to EST"),
            Tx("#foo", "Wed, 14 Jan 1970, 22:56 PST")
          )
        ),
      "@time GMT-8 produces time in GMT-8" ->
        (
          List(Rx("#foo", "bar", "@time GMT-8")),
          List(Tx("#foo", "Wed, 14 Jan 1970, 22:56 GMT-08:00"))
        ),
      "@time America/Los_Angeles produces time in America/Los_Angeles" ->
        (
          List(Rx("#foo", "bar", "@time America/Los_Angeles")),
          List(Tx("#foo", "Wed, 14 Jan 1970, 22:56 PST"))
        )
    )
