package sectery.producers

import sectery._
import sectery._
import zio.Inject._
import zio._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test.TestClock
import zio.test._

trait ProducerSpec extends DefaultRunnableSpec:

  def db: ULayer[Db.Service] = TestDb()
  def http: ULayer[Http.Service] = TestHttp()

  def pre: Option[ZIO[TestClock, Nothing, Any]] = None

  type ZStep = ZIO[TestClock, Nothing, Any]

  def specs: Map[String, (List[Rx | ZStep], List[Tx])] =
    Map.empty

  private def _specs =
    specs.toList
      .map { case (label, (rxs, txs)) =>
        test(label) {
          for
            inbox <- ZHub.sliding[Rx](100)
            outbox <- ZQueue.unbounded[Tx]
            _ <- pre.getOrElse(ZIO.succeed(()))
            fiber <- Producer
              .start(inbox, outbox)
              .inject_(db, http)
            _ <- ZIO.foreachDiscard(rxs) {
              case rx: Rx =>
                for
                  _ <- inbox.publish(rx)
                  _ <- TestClock.adjust(1.millisecond)
                yield ()
              case z: ZIO[_, _, _] => z.asInstanceOf[ZStep]
            }
            result <- outbox.takeAll
          yield assert(result)(equalTo(txs))
        } @@ timeout(2.seconds)
      }

  override def spec =
    suite(getClass().getName())(_specs: _*)
