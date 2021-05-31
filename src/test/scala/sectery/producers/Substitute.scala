package sectery.producers

import sectery._
import zio._
import zio.duration._
import zio.Inject._
import zio.test._
import zio.test.Assertion.equalTo
import zio.test.environment.TestClock
import zio.test.TestAspect._

object SubstituteSpec extends DefaultRunnableSpec:
  override def spec =
    suite(getClass().getName())(
      testM("s/bar/baz/ replaces bar with baz") {
        for
          sent   <- ZQueue.unbounded[Tx]
          inbox  <- MessageQueues.loop(new MessageLogger(sent)).inject(TestFinnhub(), TestDb(), TestHttp())
          _      <- inbox.offer(Rx("#substitute", "foo", "1: foo"))
          _      <- inbox.offer(Rx("#substitute", "bar", "2: bar"))
          _      <- inbox.offer(Rx("#substitute", "baz", "3: baz"))
          _      <- inbox.offer(Rx("#substitute", "raz", "4: raz"))
          _      <- inbox.offer(Rx("#substitute", "qux", "s/bar/baz/"))
          _      <- TestClock.adjust(1.seconds)
          m      <- sent.take
        yield assert(m)(equalTo(Tx("#substitute", "<bar> 2: baz")))
      } @@ timeout(2.seconds)
    )
