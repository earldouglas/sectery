package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test._
import zio.test.environment.TestClock

object HelpSpec extends DefaultRunnableSpec:
  override def spec =
    suite(getClass().getName())(
      test("@help produces help") {
        for
          (inbox, outbox, _) <- MessageQueues.loop
            .inject_(TestDb(), TestHttp())
          _ <- inbox.offer(Rx("#foo", "bar", "@help"))
          _ <- inbox.offer(Rx("#foo", "bar", "@help @wx"))
          _ <- inbox.offer(Rx("#foo", "bar", "@help @count"))
          _ <- inbox.offer(Rx("#foo", "bar", "@help @foo"))
          _ <- TestClock.adjust(1.seconds)
          ms <- outbox.takeAll
        yield assert(ms)(
          equalTo(
            List(
              Tx(
                "#foo",
                List(
                  "@ascii",
                  "@blink",
                  "@btc",
                  "@count",
                  "@eval",
                  "@frinkiac",
                  "@get",
                  "@grab",
                  "@ping",
                  "@quote",
                  "@set",
                  "@stock",
                  "@tell",
                  "@time",
                  "@version",
                  "@wx",
                  "s///"
                ).mkString(", ")
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
