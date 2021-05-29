package sectery

import sectery.producers._
import zio.clock.Clock
import zio.RIO
import zio.URIO
import zio.ZIO

trait Producer:

  /**
   * Run any initialization (e.g. run DDL) needed.
   */
  def init(): RIO[Db.Db with Http.Http with Clock, Unit] =
    ZIO.effectTotal(())

  /**
   * Reads an incoming message, and produces any number of responses.
   */
  def apply(m: Rx): URIO[Db.Db with Http.Http with Clock, Iterable[Tx]]

object Producer:

  private val producers: List[Producer] =
    List(
      Ping,
      Time,
      Eval,
      Html,
      Substitute,
      Count
    )

  def init(): RIO[Db.Db with Http.Http with Clock, Iterable[Unit]] =
    ZIO.foreach(producers)(_.init())

  def apply(m: Rx): URIO[Db.Db with Http.Http with Clock, Iterable[Tx]] =
    ZIO.foldLeft(producers)(List.empty) {
      (txs, p) => p.apply(m).map(_.toList).map(_ ++ txs)
    }
