package sectery

import java.io.IOException
import zio._
import zio.clock._
import zio.duration._
import zio.test._
import zio.test.Assertion.equalTo
import zio.test.environment.TestClock
import zio.test.TestAspect._
import zio.ULayer
import zio.ZLayer

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
  def spec = suite("MessageQueuesSpec")(
    testM("@ping produces pong") {
      for
        sent   <- ZQueue.unbounded[Tx]
        inbox  <- MessageQueues.loop(new MessageLogger(sent)).provideLayer(Clock.any ++ TestHttp())
        _      <- inbox.offer(Rx("#foo", "bar", "@ping"))
        _      <- TestClock.adjust(1.seconds)
        m      <- sent.take
      yield assert(m)(equalTo(Tx("#foo", "pong")))
    } @@ timeout(2.seconds),
    testM("@time produces time") {
      for
        sent   <- ZQueue.unbounded[Tx]
        inbox  <- MessageQueues.loop(new MessageLogger(sent)).provideLayer(Clock.any ++ TestHttp())
        _      <- TestClock.setTime(1234567890.millis)
        _      <- inbox.offer(Rx("#foo", "bar", "@time"))
        _      <- TestClock.adjust(1.seconds)
        m      <- sent.take
      yield assert(m)(equalTo(Tx("#foo", "Wed, 14 Jan 1970, 23:56 MST")))
    } @@ timeout(2.seconds),
    testM("@eval produces result") {
      for
        sent   <- ZQueue.unbounded[Tx]
        inbox  <- MessageQueues.loop(new MessageLogger(sent)).provideLayer(Clock.any ++ TestHttp(200, Map.empty, "42"))
        _      <- inbox.offer(Rx("#foo", "bar", "@eval 6 * 7"))
        _      <- TestClock.adjust(1.seconds)
        m      <- sent.take
      yield assert(m)(equalTo(Tx("#foo", "42")))
    } @@ timeout(2.seconds)
  )

object TestHttp:

  def apply(): ULayer[Has[Http.Service]] =
    apply(
      resStatus = 200,
      resHeaders = Map.empty,
      resBody = ""
    )

  def apply(
    resStatus: Int,
    resHeaders: Map[String, String],
    resBody: String
  ): ULayer[Has[Http.Service]] =
    ZLayer.succeed {
      new Http.Service:
        def request(
          method: String,
          url: String,
          headers: Map[String, String],
          body: Option[String]
        ): UIO[Response] =
          ZIO.effectTotal {
            Response(
              status = resStatus,
              headers = resHeaders,
              body = resBody
            )
          }
    }
