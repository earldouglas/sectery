package sectery.producers

import java.time.OffsetDateTime
import java.time.ZoneOffset
import sectery._
import zio.Inject._
import zio._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test.TestClock
import zio.test._

object TellSpec extends DefaultRunnableSpec:
  override def spec =
    suite(getClass().getName())(
      test("@tell leaves a message") {
        for
          _ <- TestClock.setDateTime {
            val zo = OffsetDateTime.now().getOffset()
            OffsetDateTime.of(1970, 2, 11, 23, 59, 59, 0, zo)
          }
          (inbox, outbox, _) <- MessageQueues.loop
            .inject_(TestDb(), TestHttp())
          _ <- inbox.offer(Rx("#foo", "user1", "@tell user2 Howdy!"))
          _ <- TestClock.adjust(1.seconds)
          _ <- inbox.offer(Rx("#foo", "user1", "@tell user2 Hi there!"))
          _ <- inbox.offer(Rx("#foo", "user2", "Hey"))
          _ <- TestClock.adjust(1.seconds)
          ms <- outbox.takeAll
        yield assert(ms)(
          equalTo(
            List(
              Tx("#foo", "I will let them know."),
              Tx("#foo", "I will let them know."),
              Tx("#foo", "user2: on 1970-02-11, user1 said: Howdy!"),
              Tx("#foo", "user2: on 1970-02-12, user1 said: Hi there!")
            )
          )
        )
      } @@ timeout(2.seconds)
    )
