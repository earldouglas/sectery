package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test._
import zio.test.environment.TestClock

object GrabSpec extends DefaultRunnableSpec:
  override def spec =
    suite(getClass().getName())(
      test("@count produces count") {
        for
          sent <- ZQueue.unbounded[Tx]
          (inbox, _) <- MessageQueues
            .loop(new MessageLogger(sent))
            .inject_(TestDb(), TestHttp())
          _ <- inbox.offer(Rx("#foo", "bar", "@grab baz"))
          _ <- inbox.offer(Rx("#foo", "baz", "Howdy"))
          _ <- inbox.offer(Rx("#foo", "baz", "Hey"))
          _ <- inbox.offer(Rx("#foo", "bar", "@quote baz"))
          _ <- inbox.offer(Rx("#foo", "bar", "@grab baz"))
          _ <- inbox.offer(Rx("#foo", "bar", "@quote baz"))
          _ <- TestClock.adjust(1.seconds)
          ms <- sent.takeAll
        yield assert(ms)(
          equalTo(
            List(
              Tx("#foo", "baz hasn't said anything."),
              Tx("#foo", "baz hasn't said anything."),
              Tx("#foo", "Grabbed baz."),
              Tx("#foo", "<baz> Hey")
            )
          )
        )
      } @@ timeout(2.seconds)
    )
