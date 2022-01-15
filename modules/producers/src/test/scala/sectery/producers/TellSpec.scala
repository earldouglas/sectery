package sectery.producers

import java.time.OffsetDateTime
import sectery._
import zio._
import zio.test.TestClock

object TellSpec extends ProducerSpec:

  override val pre =
    Some(
      TestClock.setDateTime {
        val zo = OffsetDateTime.now().getOffset()
        OffsetDateTime.of(1970, 2, 11, 23, 59, 59, 0, zo)
      }
    )

  override val specs =
    Map(
      "@tell leaves a message" ->
        (
          List(
            Rx("#foo", "user1", "@tell user2 Howdy!"),
            TestClock.adjust(1.seconds),
            Rx("#foo", "user1", "@tell user2 Hi there!"),
            TestClock.adjust(1.seconds),
            Rx("#foo", "user2", "Hey")
          ),
          List(
            Tx("#foo", "I will let them know."),
            Tx("#foo", "I will let them know."),
            Tx("#foo", "user2: 5 decades ago, user1 said: Howdy!"),
            Tx("#foo", "user2: 5 decades ago, user1 said: Hi there!")
          )
        )
    )
