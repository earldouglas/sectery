package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test._
import zio.test.environment.TestClock

object TimeSpec extends DefaultRunnableSpec:
  override def spec =
    suite(getClass().getName())(
      test("@time produces time") {
        for
          (inbox, outbox, _) <- MessageQueues.loop
            .inject_(TestDb(), TestHttp())
          _ <- TestClock.setTime(1234567890.millis)
          _ <- inbox.offer(Rx("#foo", "bar", "@time"))
          _ <- TestClock.adjust(1.seconds)
          m <- outbox.take
        yield assert(m)(
          equalTo(Tx("#foo", "Wed, 14 Jan 1970, 23:56 MST"))
        )
      } @@ timeout(2.seconds)
    )
