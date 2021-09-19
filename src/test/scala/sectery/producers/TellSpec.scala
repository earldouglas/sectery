package sectery.producers

import java.time.OffsetDateTime
import java.time.ZoneOffset
import sectery._
import zio.Inject._
import zio._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test._
import zio.test.environment.TestClock

object TellSpec extends DefaultRunnableSpec:
  override def spec =
    suite(getClass().getName())(
      test("@tell leaves a message") {
        for
          _ <- TestClock.setDateTime {
            val zo = OffsetDateTime.now().getOffset()
            OffsetDateTime.of(1970, 2, 11, 0, 0, 0, 0, zo)
          }
          sent <- ZQueue.unbounded[Tx]
          (inbox, _) <- MessageQueues
            .loop(new MessageLogger(sent))
            .inject_(TestDb(), TestHttp())
          _ <- inbox.offer(Rx("#foo", "user1", "@tell user2 Howdy!"))
          _ <- TestClock.adjust(1.days)
          _ <- inbox.offer(Rx("#foo", "user1", "@tell user2 Hi there!"))
          _ <- inbox.offer(Rx("#foo", "user2", "Hey"))
          _ <- TestClock.adjust(1.seconds)
          ms <- sent.takeAll
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
