package sectery

import java.io.IOException
import zio._
import zio.clock._
import zio.duration._
import zio.test._
import zio.test.Assertion.equalTo
import zio.test.environment.TestClock

/**
 * A [[Sender]] implementation for testing.  Saves "sent" messages to a
 * queue for later inspection.
 */
class MessageLogger(queue: Queue[Tx]) extends Sender {
  def send(m: Tx): UIO[Unit] =
    queue.offer(m) *> ZIO.unit
}

/**
 * Tests the management of the receive and send message queues.
 */
object MessageQueuesSpec extends DefaultRunnableSpec {
  def spec = suite("MessageQueuesSpec")(
    testM("@ping produces pong") {
      for
        sent   <- ZQueue.unbounded[Tx]
        inbox  <- MessageQueues.loop(new MessageLogger(sent))
        _      <- inbox.offer(Rx("#foo", "bar", "@ping"))
        _      <- TestClock.adjust(1.seconds)
        m      <- sent.take
      yield assert(m)(equalTo(Tx("#foo", "pong")))
    },
    testM("@time produces time") {
      for
        sent   <- ZQueue.unbounded[Tx]
        inbox  <- MessageQueues.loop(new MessageLogger(sent))
        _      <- TestClock.setTime(1234567890.millis)
        _      <- inbox.offer(Rx("#foo", "bar", "@time"))
        _      <- TestClock.adjust(1.seconds)
        m      <- sent.take
      yield assert(m)(equalTo(Tx("#foo", "Wed, 14 Jan 1970, 23:56 MST")))
    }
  )
}
