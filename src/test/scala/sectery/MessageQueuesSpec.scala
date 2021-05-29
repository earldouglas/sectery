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

  implicit class Inject2[R0, R1, E, A](z: ZIO[Has[R0] with Has[R1], E, A])(implicit t: Tag[Has[R1]]):
    def inject(r0: ULayer[Has[R0]]): ZIO[Has[R1], E, A] =
      ZIO.fromFunctionM { r =>
        z.provideLayer(r0 ++ ZLayer.succeedMany(r))
      }

  implicit class Inject3[R0, R1, R2, E, A](z: ZIO[Has[R0] with Has[R1] with Has[R2], E, A])(implicit t1: Tag[Has[R1]], t2: Tag[Has[R2]]):
    def inject(r0: ULayer[Has[R0]], r1: ULayer[Has[R1]]): ZIO[Has[R2], E, A] =
      ZIO.fromFunctionM { r =>
        z.provideLayer(r0 ++ r1 ++ ZLayer.succeedMany(r))
      }

  def spec = suite("MessageQueuesSpec")(
    testM("@ping produces pong") {
      for
        sent   <- ZQueue.unbounded[Tx]
        inbox  <- MessageQueues.loop(new MessageLogger(sent)).inject(TestDb(), TestHttp())
        _      <- inbox.offer(Rx("#foo", "bar", "@ping"))
        _      <- TestClock.adjust(1.seconds)
        m      <- sent.take
      yield assert(m)(equalTo(Tx("#foo", "pong")))
    } @@ timeout(2.seconds),
    testM("@time produces time") {
      for
        sent   <- ZQueue.unbounded[Tx]
        inbox  <- MessageQueues.loop(new MessageLogger(sent)).inject(TestDb(), TestHttp())
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
        inbox  <- MessageQueues.loop(new MessageLogger(sent)).inject(TestDb(), http)
        _      <- inbox.offer(Rx("#foo", "bar", "@eval 6 * 7"))
        _      <- TestClock.adjust(1.seconds)
        m      <- sent.take
      yield assert(m)(equalTo(Tx("#foo", "42")))
    } @@ timeout(2.seconds),
    testM("URL produces description from HTML") {
      for
        sent   <- ZQueue.unbounded[Tx]
        http    = sys.env.get("TEST_HTTP_LIVE") match
                    case Some("true") => Http.live
                    case _ =>
                      val http: ULayer[Has[Http.Service]] =
                        ZLayer.succeed {
                          new Http.Service:
                            def request(
                              method: String,
                              url: String,
                              headers: Map[String, String],
                              body: Option[String]
                            ): UIO[Response] =
                              url match
                                case "https://earldouglas.com/posts/scala.html" =>
                                  ZIO.effectTotal {
                                    Response(
                                      status = 200,
                                      headers = Map.empty,
                                      body = """|<html>
                                                |  <head>
                                                |    <meta   name="description"   content="Some notes on the Scala language, 
                                                |libraries, and ecosystem."  >
                                                |  </head>
                                                |</html>
                                                |""".stripMargin
                                    )
                                  }
                                case _ =>
                                  ZIO.effectTotal {
                                    Response(
                                      status = 404,
                                      headers = Map.empty,
                                      body = ""
                                    )
                                  }
                        }
                      http
        inbox  <- MessageQueues.loop(new MessageLogger(sent)).inject(TestDb(), http)
        _      <- inbox.offer(Rx("#foo", "bar", "foo bar https://earldouglas.com/posts/scala.html baz"))
        _      <- TestClock.adjust(1.seconds)
        m      <- sent.take
      yield assert(m)(equalTo(Tx("#foo", "Some notes on the Scala language, libraries, and ecosystem.")))
    } @@ timeout(2.seconds),
    testM("s/bar/baz/ replaces bar with baz") {
      for
        sent   <- ZQueue.unbounded[Tx]
        inbox  <- MessageQueues.loop(new MessageLogger(sent)).inject(TestDb(), TestHttp())
        _      <- inbox.offer(Rx("#foo", "foo", "1: foo"))
        _      <- inbox.offer(Rx("#foo", "bar", "2: bar"))
        _      <- inbox.offer(Rx("#foo", "baz", "3: baz"))
        _      <- inbox.offer(Rx("#foo", "raz", "4: raz"))
        _      <- inbox.offer(Rx("#foo", "qux", "s/bar/baz/"))
        _      <- TestClock.adjust(1.seconds)
        m      <- sent.take
      yield assert(m)(equalTo(Tx("#foo", "<bar> 2: baz")))
    } @@ timeout(2.seconds),
    testM("@count produces count") {
      for
        sent   <- ZQueue.unbounded[Tx]
        inbox  <- MessageQueues.loop(new MessageLogger(sent)).inject(TestDb(), TestHttp())
        _      <- inbox.offer(Rx("#foo", "bar", "@count"))
        _      <- inbox.offer(Rx("#foo", "bar", "@count"))
        _      <- inbox.offer(Rx("#foo", "bar", "@count"))
        _      <- TestClock.adjust(1.seconds)
        ms     <- sent.takeAll
      yield
        assert(ms)(equalTo(List(Tx("#foo", "1"), Tx("#foo", "2"), Tx("#foo", "3"))))
    } @@ timeout(2.seconds)
  )
