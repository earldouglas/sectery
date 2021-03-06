package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.duration._
import zio.test.Assertion.matchesRegex
import zio.test.TestAspect._
import zio.test._
import zio.test.environment.TestClock

object VersionSpec extends DefaultRunnableSpec:
  override def spec =
    suite(getClass().getName())(
      testM("@version produces version") {
        for
          sent <- ZQueue.unbounded[Tx]
          inbox <- MessageQueues
            .loop(new MessageLogger(sent))
            .inject(TestDb(), TestHttp())
          _ <- inbox.offer(Rx("#foo", "bar", "@version"))
          _ <- TestClock.adjust(1.seconds)
          m <- sent.take
        yield assert(m.message)(
          matchesRegex("^[0-9a-f]{40}(-SNAPSHOT)?$")
        )
      } @@ timeout(2.seconds)
    )
