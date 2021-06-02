package sectery.producers

import sectery._
import zio._
import zio.duration._
import zio.Inject._
import zio.test._
import zio.test.Assertion.equalTo
import zio.test.environment.TestClock
import zio.test.TestAspect._

object StockSpec extends DefaultRunnableSpec:
  override def spec =
    suite(getClass().getName())(
      testM("@stock FOO produces quote") {
        for
          sent   <- ZQueue.unbounded[Tx]
          fh      = sys.env.get("TEST_FINNHUB_LIVE") match
                      case Some("true") => Finnhub.live
                      case _ => TestFinnhub()
          inbox  <- MessageQueues.loop(new MessageLogger(sent)).inject(fh, TestDb(), TestHttp())
          _      <- inbox.offer(Rx("#foo", "bar", "@stock FOO"))
          _      <- inbox.offer(Rx("#foo", "bar", "@stock foo"))
          _      <- inbox.offer(Rx("#foo", "bar", "@stock BAR"))
          _      <- TestClock.adjust(1.seconds)
          m1     <- sent.take
          m2     <- sent.take
          m3     <- sent.take
        yield assert((m1, m2, m3))(equalTo((
          Tx("#foo", "FOO: 6.00 +2.00 (+50.00%)"),
          Tx("#foo", "foo: 6.00 +2.00 (+50.00%)"),
          Tx("#foo", "BAR: stonk not found")
        )))
      } @@ timeout(2.seconds)
    )
