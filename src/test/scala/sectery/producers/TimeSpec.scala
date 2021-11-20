package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test.TestClock
import zio.test._

object TimeSpec extends DefaultRunnableSpec:
  override def spec =
    suite(getClass().getName())(
      test("@time produces server time") {
        for
          _ <- TestClock.setTime(1234567890.millis)
          (inbox, outbox, _) <- MessageQueues.loop
            .inject_(TestDb(), TestHttp())
          _ <- inbox.offer(Rx("#foo", "bar", "@time"))
          _ <- TestClock.adjust(1.seconds)
          m <- outbox.take
        yield assert(m)(
          equalTo(Tx("#foo", "Wed, 14 Jan 1970, 23:56 MST"))
        )
      } @@ timeout(2.seconds),
      test("@time produces time in user-configured tz") {
        for
          _ <- TestClock.setTime(1234567890.millis)
          (inbox, outbox, _) <- MessageQueues.loop
            .inject_(TestDb(), TestHttp())
          _ <- inbox.offer(Rx("#foo", "bar", "@time"))
          _ <- inbox.offer(Rx("#foo", "bar", "@set tz PST"))
          _ <- inbox.offer(Rx("#foo", "bar", "@time"))
          _ <- TestClock.adjust(1.seconds)
          ms <- outbox.takeAll
        yield assert(ms)(
          equalTo(
            List(
              Tx("#foo", "Wed, 14 Jan 1970, 23:56 MST"),
              Tx("#foo", "bar: tz set to PST"),
              Tx("#foo", "Wed, 14 Jan 1970, 22:56 PST")
            )
          )
        )
      } @@ timeout(2.seconds),
      test("@time PST produces time in PST") {
        for
          _ <- TestClock.setTime(1234567890.millis)
          (inbox, outbox, _) <- MessageQueues.loop
            .inject_(TestDb(), TestHttp())
          _ <- inbox.offer(Rx("#foo", "bar", "@time PST"))
          _ <- inbox.offer(Rx("#foo", "bar", "@set tz EST"))
          _ <- inbox.offer(Rx("#foo", "bar", "@time PST"))
          _ <- TestClock.adjust(1.seconds)
          ms <- outbox.takeAll
        yield assert(ms)(
          equalTo(
            List(
              Tx("#foo", "Wed, 14 Jan 1970, 22:56 PST"),
              Tx("#foo", "bar: tz set to EST"),
              Tx("#foo", "Wed, 14 Jan 1970, 22:56 PST")
            )
          )
        )
      } @@ timeout(2.seconds),
      test("@time GMT-8 produces time in GMT-8") {
        for
          _ <- TestClock.setTime(1234567890.millis)
          (inbox, outbox, _) <- MessageQueues.loop
            .inject_(TestDb(), TestHttp())
          _ <- inbox.offer(Rx("#foo", "bar", "@time GMT-8"))
          _ <- TestClock.adjust(1.seconds)
          m <- outbox.take
        yield assert(m)(
          equalTo(Tx("#foo", "Wed, 14 Jan 1970, 22:56 GMT-08:00"))
        )
      } @@ timeout(2.seconds),
      test(
        "@time America/Los_Angeles produces time in America/Los_Angeles"
      ) {
        for
          _ <- TestClock.setTime(1234567890.millis)
          (inbox, outbox, _) <- MessageQueues.loop
            .inject_(TestDb(), TestHttp())
          _ <- inbox.offer(
            Rx("#foo", "bar", "@time America/Los_Angeles")
          )
          _ <- TestClock.adjust(1.seconds)
          m <- outbox.take
        yield assert(m)(
          equalTo(Tx("#foo", "Wed, 14 Jan 1970, 22:56 PST"))
        )
      } @@ timeout(2.seconds)
    )
