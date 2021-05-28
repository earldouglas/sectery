package sectery

import zio._
import zio.duration._
import zio.test._
import zio.test.Assertion.equalTo
import zio.test.environment.TestClock
import zio.test.TestAspect._

/**
 * A [[Sender]] implementation for testing.  Saves "sent" messages to a
 * queue for later inspection.
 */
class MessageLogger(queue: Queue[Tx]) extends Sender:
  def send(m: Tx): UIO[Unit] =
    queue.offer(m) *> ZIO.unit

/**
 * Tests the management of the receive and send message queues.
 */
object MessageQueuesSpec extends DefaultRunnableSpec:

  implicit class Inject[R0, R1, E, A](z: ZIO[Has[R0] with Has[R1], E, A])(implicit t: Tag[Has[R1]]):
    def inject(r0: ULayer[Has[R0]]): ZIO[Has[R1], E, A] =
      ZIO.fromFunctionM { r1 =>
        z.provideLayer(r0 ++ ZLayer.succeedMany(r1))
      }

  def spec = suite("MessageQueuesSpec")(
    testM("@ping produces pong") {
      for
        sent   <- ZQueue.unbounded[Tx]
        inbox  <- MessageQueues.loop(new MessageLogger(sent)).inject(TestHttp())
        _      <- inbox.offer(Rx("#foo", "bar", "@ping"))
        _      <- TestClock.adjust(1.seconds)
        m      <- sent.take
      yield assert(m)(equalTo(Tx("#foo", "pong")))
    } @@ timeout(2.seconds),
    testM("@time produces time") {
      for
        sent   <- ZQueue.unbounded[Tx]
        inbox  <- MessageQueues.loop(new MessageLogger(sent)).inject(TestHttp())
        _      <- TestClock.setTime(1234567890.millis)
        _      <- inbox.offer(Rx("#foo", "bar", "@time"))
        _      <- TestClock.adjust(1.seconds)
        m      <- sent.take
      yield assert(m)(equalTo(Tx("#foo", "Wed, 14 Jan 1970, 23:56 MST")))
    } @@ timeout(2.seconds),
    testM("@eval produces result") {
      for
        sent   <- ZQueue.unbounded[Tx]
        http    = sys.env.get("TEST_HTTP_LIVE") match
                    case Some("true") => Http.live
                    case _ => TestHttp(200, Map.empty, "42")
        inbox  <- MessageQueues.loop(new MessageLogger(sent)).inject(http)
        _      <- inbox.offer(Rx("#foo", "bar", "@eval 6 * 7"))
        _      <- TestClock.adjust(1.seconds)
        m      <- sent.take
      yield assert(m)(equalTo(Tx("#foo", "42")))
    } @@ timeout(2.seconds)
  )
