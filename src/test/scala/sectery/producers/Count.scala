package sectery.producers

import sectery._
import zio._
import zio.duration._
import zio.Inject._
import zio.test._
import zio.test.Assertion.equalTo
import zio.test.environment.TestClock
import zio.test.TestAspect._

object CountSpec extends DefaultRunnableSpec:
  override def spec =
    suite(getClass().getName())(
      testM("@ping produces pong") {
        for
          sent   <- ZQueue.unbounded[Tx]
          inbox  <- MessageQueues.loop(new MessageLogger(sent)).inject(TestDb(), TestHttp())
          _      <- inbox.offer(Rx("#foo", "bar", "@ping"))
          _      <- TestClock.adjust(1.seconds)
          m      <- sent.take
        yield assert(m)(equalTo(Tx("#foo", "pong")))
      } @@ timeout(2.seconds)
    )
