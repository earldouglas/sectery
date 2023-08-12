package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.stream.ZSink
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test.TestClock
import zio.test._

object ProducerSpec:

  lazy val useRabbitMQ: Boolean =
    if sys.env.contains("RABBIT_MQ_HOSTNAME") &&
      sys.env.contains("RABBIT_MQ_PORT") &&
      sys.env.contains("RABBIT_MQ_USERNAME") &&
      sys.env.contains("RABBIT_MQ_PASSWORD")
    then
      println("Testing with RabbitMQ")
      true
    else
      println("Testing with TestMQ")
      false

trait ProducerSpec extends ZIOSpecDefault:

  def testDb: ULayer[Db.Service] = TestDb()
  def http: ULayer[Http.Service] = TestHttp()

  def pre: Option[ZIO[TestClock, Nothing, Any]] = None

  type ZStep = ZIO[Db.Db & TestClock, Throwable, Any]

  def specs: Map[String, (List[Rx | ZStep], List[Tx])] =
    Map.empty

  private def _specs: List[Spec[Live & Annotations, Throwable]] =
    specs.toList
      .map { case (label, (rxs, txs)) =>
        test(label) {
          {
            for
              _ <- pre.getOrElse(ZIO.succeed(()))
              mq <-
                if ProducerSpec.useRabbitMQ
                then ZIO.succeed(sectery.RabbitMQ(label))
                else TestMQ()
              db = testDb
              fiber <- Producer.start
                .inject_(mq, db, http)
              _ <- ZIO.foreachDiscard(rxs) {
                case rx: Rx =>
                  for
                    inbox <- MessageQueue.inbox.inject_(mq)
                    _ <- inbox.offer(Some(rx))
                    _ <- TestClock.adjust(1.millisecond)
                  yield ()
                case z: ZIO[_, _, _] =>
                  z.asInstanceOf[ZStep].inject_(db)
              }
              outbox <- MessageQueue.outbox.inject_(mq)
              result <- outbox.take.run(ZSink.collectAllN(txs.length))
            yield assert(result)(equalTo(txs))
          }.provideLayer(TestClock.default)
        } @@ timeout(2.seconds)
      }

  override def spec =
    suite(getClass().getName())(_specs: _*)
