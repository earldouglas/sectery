package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test.TestClock
import zio.test._

trait ProducerSpec extends DefaultRunnableSpec:

  def testDb: ULayer[Db.Service] = TestDb()
  def http: ULayer[Http.Service] = TestHttp()

  def pre: Option[ZIO[TestClock, Nothing, Any]] = None

  type ZStep = ZIO[Db.Db with TestClock, Throwable, Any]

  def specs: Map[String, (List[Rx | ZStep], List[Tx])] =
    Map.empty

  private def _specs =
    specs.toList
      .map { case (label, (rxs, txs)) =>
        test(label) {
          for
            inbox <- ZQueue.unbounded[Rx].map(new TestQueue(_))
            outbox <- ZQueue.unbounded[Tx].map(new TestQueue(_))
            _ <- pre.getOrElse(ZIO.succeed(()))
            db = testDb
            fiber <- Producer
              .start(inbox, outbox)
              .inject_(db, http)
            _ <- ZIO.foreachDiscard(rxs) {
              case rx: Rx =>
                for
                  _ <- inbox.offer(Some(rx))
                  _ <- TestClock.adjust(1.millisecond)
                yield ()
              case z: ZIO[_, _, _] =>
                z.asInstanceOf[ZStep].inject_(db)
            }
            result <- outbox.q.takeAll
          yield assert(result)(equalTo(txs))
        } @@ timeout(2.seconds)
      }

  override def spec =
    suite(getClass().getName())(_specs: _*)
