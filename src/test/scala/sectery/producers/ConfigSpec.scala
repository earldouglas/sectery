package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test._
import zio.test.environment.TestClock

object ConfigSpec extends DefaultRunnableSpec:
  override def spec =
    suite(getClass().getName())(
      test("@set and @get tz") {
        for
          (inbox, outbox, _) <- MessageQueues.loop
            .inject_(TestDb(), TestHttp())
          _ <- inbox.offer(Rx("#foo", "bar", "@get tz"))
          _ <- inbox.offer(Rx("#foo", "bar", "@set tz EST"))
          _ <- inbox.offer(Rx("#foo", "bar", "@get tz"))
          _ <- inbox.offer(Rx("#foo", "baz", "@get tz"))
          _ <- inbox.offer(Rx("#foo", "baz", "@set tz PST"))
          _ <- inbox.offer(Rx("#foo", "bar", "@get tz"))
          _ <- inbox.offer(Rx("#foo", "baz", "@get tz"))
          _ <- inbox.offer(Rx("#foo", "bar", "@set foo bar baz"))
          _ <- inbox.offer(Rx("#foo", "bar", "@get foo"))
          _ <- TestClock.adjust(1.seconds)
          ms <- outbox.takeAll
        yield assert(ms)(
          equalTo(
            List(
              Tx("#foo", "bar: tz is not set"),
              Tx("#foo", "bar: tz set to EST"),
              Tx("#foo", "bar: tz is set to EST"),
              Tx("#foo", "baz: tz is not set"),
              Tx("#foo", "baz: tz set to PST"),
              Tx("#foo", "bar: tz is set to EST"),
              Tx("#foo", "baz: tz is set to PST"),
              Tx("#foo", "bar: foo set to bar baz"),
              Tx("#foo", "bar: foo is set to bar baz")
            )
          )
        )
      } @@ timeout(2.seconds)
    )
