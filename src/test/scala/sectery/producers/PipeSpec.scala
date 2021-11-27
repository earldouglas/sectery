package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test.TestClock
import zio.test._

object PipeSpec extends DefaultRunnableSpec:
  override def spec =
    suite(getClass().getName())(
      test("pipe output into input") {
        for
          (inbox, outbox, _) <- MessageQueues.loop
            .inject_(TestDb(), TestHttp())
          _ <- inbox.offer(
            Rx("#foo", "bar", "@morse encode foo | @blink")
          )
          _ <- inbox.offer(
            Rx("#foo", "bar", "@morse encode foo | @morse decode")
          )
          _ <- inbox.offer(
            Rx(
              "#foo",
              "bar",
              "@morse encode foo | @morse decode | @morse encode"
            )
          )
          _ <- TestClock.adjust(1.seconds)
          ms <- outbox.takeAll
        yield assert(ms)(
          equalTo(
            List(
              Tx("#foo", "\u0006..-. --- ---\u0006"),
              Tx("#foo", "FOO"),
              Tx("#foo", "..-. --- ---")
            )
          )
        )
      } @@ timeout(2.seconds)
    )
