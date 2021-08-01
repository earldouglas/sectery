package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test._
import zio.test.environment.TestClock

object PingSpec extends DefaultRunnableSpec:
  override def spec =
    suite(getClass().getName())(
      test("@ping produces pong") {
        for
          sent <- ZQueue.unbounded[Tx]
          (inbox, _) <- MessageQueues
            .loop(new MessageLogger(sent))
            .inject_(TestDb(), TestHttp())
          _ <- inbox.offer(Rx("#foo", "bar", "@ping"))
          _ <- TestClock.adjust(1.seconds)
          m <- sent.take
        yield assert(m)(equalTo(Tx("#foo", "pong")))
      } @@ timeout(2.seconds)
    )
