package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test._
import zio.test.environment.TestClock

object CountSpec extends DefaultRunnableSpec:
  override def spec =
    suite(getClass().getName())(
      test("@count produces count") {
        for
          sent <- ZQueue.unbounded[Tx]
          inbox <- MessageQueues
            .loop(new MessageLogger(sent))
            .inject_(TestDb(), TestHttp())
          _ <- inbox.offer(Rx("#foo", "bar", "@count"))
          _ <- inbox.offer(Rx("#foo", "bar", "@count"))
          _ <- inbox.offer(Rx("#foo", "bar", "@count"))
          _ <- TestClock.adjust(1.seconds)
          ms <- sent.takeAll
        yield assert(ms)(
          equalTo(
            List(Tx("#foo", "1"), Tx("#foo", "2"), Tx("#foo", "3"))
          )
        )
      } @@ timeout(2.seconds)
    )
