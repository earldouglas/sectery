package sectery

import sectery.producers._
import zio.URIO
import zio.ZIO
import zio.clock.Clock

/**
 * Reads an incoming message, and produces any number of responses.
 */
trait Producer:
  def apply(m: Rx): URIO[Clock, Iterable[Tx]]

object Producer:

  private val producers: List[Producer] =
    List(
      Ping,
      Time
    )

  def apply(m: Rx): URIO[Clock, Iterable[Tx]] =
    ZIO.foldLeft(producers)(List.empty) {
      (txs, p) => p.apply(m).map(_.toList).map(_ ++ txs)
    }
