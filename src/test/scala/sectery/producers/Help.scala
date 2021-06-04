package sectery.producers

import sectery._
import zio._
import zio.duration._
import zio.Inject._
import zio.test._
import zio.test.Assertion.equalTo
import zio.test.environment.TestClock
import zio.test.TestAspect._

object HelpSpec extends DefaultRunnableSpec:
  override def spec =
    suite(getClass().getName())(
    testM("@help produces help") {
      for
        sent   <- ZQueue.unbounded[Tx]
        inbox  <- MessageQueues.loop(new MessageLogger(sent)).inject(TestFinnhub(), TestDb(), TestHttp())
        _      <- inbox.offer(Rx("#foo", "bar", "@help"))
        _      <- TestClock.adjust(1.seconds)
        ms     <- sent.takeAll
      yield
        assert(ms)(equalTo(List(Tx("#foo", "Available commands: @count, @eval, @ping, @stock, @time, @wx"))))
      } @@ timeout(2.seconds)
    )
