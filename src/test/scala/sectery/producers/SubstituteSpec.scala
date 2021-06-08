package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.duration._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test._
import zio.test.environment.TestClock

object SubstituteSpec extends DefaultRunnableSpec:
  override def spec =
    suite(getClass().getName())(
      testM("s/bar/baz/ replaces bar with baz") {
        for
          sent <- ZQueue.unbounded[Tx]
          inbox <- MessageQueues
            .loop(new MessageLogger(sent))
            .inject(TestDb(), TestHttp())
          _ <- inbox.offer(
            Rx("#substitute", "foo", "I said bar first.")
          )
          _ <- TestClock.adjust(
            1.millis
          ) // timestamps in db need to differ by at least a millisecond
          _ <- inbox.offer(
            Rx("#substitute", "bar", "I said bar second.")
          )
          _ <- TestClock.adjust(
            1.millis
          ) // timestamps in db need to differ by at least a millisecond
          _ <- inbox.offer(
            Rx("#substitute", "baz", "I said bar third.")
          )
          _ <- TestClock.adjust(
            1.millis
          ) // timestamps in db need to differ by at least a millisecond
          _ <- inbox.offer(
            Rx("#substitute", "raz", "I didn't say it at all.")
          )
          _ <- TestClock.adjust(
            1.millis
          ) // timestamps in db need to differ by at least a millisecond
          _ <- inbox.offer(Rx("#substitute", "qux", "s/bar/baz/"))
          _ <- TestClock.adjust(1.seconds)
          m <- sent.take
        yield assert(m)(
          equalTo(Tx("#substitute", "<baz> I said baz third."))
        )
      } @@ timeout(2.seconds)
    )
