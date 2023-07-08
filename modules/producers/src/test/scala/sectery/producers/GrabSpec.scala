package sectery.producers

import java.time.OffsetDateTime
import java.time.ZoneId
import sectery._
import zio._
import zio.test.TestClock
import zio.test._

object GrabSpec extends ProducerSpec:

  override val pre =
    Some(
      TestClock.setTime {
        val zo = OffsetDateTime.now(ZoneId.of("UTC-7")).getOffset()
        OffsetDateTime.of(1970, 2, 11, 12, 0, 0, 0, zo).toInstant()
      }
    )

  override val specs =
    Map(
      "@quote quotes grabbed nick" ->
        (
          List(
            Rx("#foo", "bar", "@grab baz"),
            Rx("#foo", "baz", "Howdy"),
            Rx("#foo", "baz", "Hey"),
            Rx("#foo", "bar", "@quote baz"),
            Rx("#foo", "bar", "@quote"),
            Rx("#foo", "bar", "@grab bar"),
            Rx("#foo", "bar", "@grab baz"),
            Rx("#foo", "bar", "@quote baz"),
            Rx("#foo", "bar", "<raz>@quote"),
            Rx("#foo", "baz", "@grab bar"),
            Rx("#foo", "baz", "@quote bar")
          ),
          List(
            Tx("#foo", "baz hasn't said anything."),
            Tx("#foo", "baz hasn't said anything."),
            Tx("#foo", "Nobody has said anything."),
            Tx("#foo", "You can't grab yourself."),
            Tx("#foo", "Grabbed baz."),
            Tx("#foo", "<baz> Hey"),
            Tx("#foo", "<baz> Hey"),
            Tx("#foo", "Grabbed bar."),
            Tx("#foo", "<bar> <raz>@quote")
          )
        ),
      "autoquote once an hour" ->
        (
          List(
            Rx("#foo", "baz", "Hey"),
            Rx("#foo", "bar", "@grab baz"),
            TestClock.adjust(65.minutes)
          ),
          List(
            Tx("#foo", "Grabbed baz."),
            Tx("#foo", "<baz> Hey")
          )
        )
    )
