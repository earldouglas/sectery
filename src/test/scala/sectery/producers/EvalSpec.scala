package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test.TestClock
import zio.test._

object EvalSpec extends DefaultRunnableSpec:
  override def spec =
    suite(getClass().getName())(
      test("@eval produces result") {
        for
          _ <- ZIO.succeed(())
          http = sys.env.get("TEST_HTTP_LIVE") match
            case Some("true") => Http.live
            case _            => TestHttp(200, Map.empty, "42")
          (inbox, outbox, _) <- MessageQueues.loop
            .inject_(TestDb(), http)
          _ <- inbox.offer(Rx("#foo", "bar", "@eval 6 * 7"))
          _ <- TestClock.adjust(1.seconds)
          m <- outbox.take
        yield assert(m)(equalTo(Tx("#foo", "42")))
      } @@ timeout(2.seconds)
    )
