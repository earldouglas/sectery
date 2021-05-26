package sectery

import sectery.producers._
import zio.clock.Clock
import zio.Has
import zio.URIO
import zio.ZIO

/**
 * Reads an incoming message, and produces any number of responses.
 */
trait Producer:
  def apply(m: Rx): URIO[Clock with Http.Http, Iterable[Tx]]

object Producer:

  private val producers: List[Producer] =
    List(
      Ping,
      Time,
      Eval
    )

  def apply(m: Rx): URIO[Clock with Http.Http, Iterable[Tx]] =
    ZIO.foldLeft(producers)(List.empty) {
      (txs, p) => p.apply(m).map(_.toList).map(_ ++ txs)
    }
