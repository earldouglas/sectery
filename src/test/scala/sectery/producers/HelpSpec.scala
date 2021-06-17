package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.duration._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test._
import zio.test.environment.TestClock

object HelpSpec extends DefaultRunnableSpec:
  override def spec =
    suite(getClass().getName())(
      testM("@help produces help") {
        for
          sent <- ZQueue.unbounded[Tx]
          inbox <- MessageQueues
            .loop(new MessageLogger(sent))
            .inject(TestDb(), TestHttp())
          _ <- inbox.offer(Rx("#foo", "bar", "@help"))
          _ <- inbox.offer(Rx("#foo", "bar", "@help @wx"))
          _ <- inbox.offer(Rx("#foo", "bar", "@help @count"))
          _ <- inbox.offer(Rx("#foo", "bar", "@help @foo"))
          _ <- TestClock.adjust(1.seconds)
          ms <- sent.takeAll
        yield assert(ms)(
          equalTo(
            List(
              Tx(
                "#foo",
                "@btc, @count, @eval, @ping, @stock, @time, @version, @wx, s///"
              ),
              Tx(
                "#foo",
                "Usage: @wx <location>, e.g. @wx san francisco"
              ),
              Tx("#foo", "Usage: @count"),
              Tx("#foo", "I don't know anything about @foo")
            )
          )
        )
      } @@ timeout(2.seconds)
    )
