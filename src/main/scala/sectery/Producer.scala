package sectery

import sectery.producers._
import zio.clock.Clock
import zio.RIO
import zio.UIO
import zio.URIO
import zio.ZIO

trait Producer:

  /**
   * List of @commands that this producer responds to.
   */
  def help(): Iterable[String]

  /**
   * Run any initialization (e.g. run DDL) needed.
   */
  def init(): RIO[Producer.Env, Unit] =
    ZIO.effectTotal(())

  /**
   * Reads an incoming message, and produces any number of responses.
   */
  def apply(m: Rx): URIO[Producer.Env, Iterable[Tx]]

class Help(producers: List[Producer]) extends Producer:

  private val helpMessage: String =
    s"""Available commands: ${producers.flatMap(p => p.help()).sorted.mkString(", ")}"""

  override def help(): Iterable[String] =
    None

  override def apply(m: Rx): UIO[Iterable[Tx]] =
    ZIO.succeed {
      m match
        case Rx(channel, _, "@help") =>
          Some(Tx(m.channel, helpMessage))
        case _ =>
          None
    }

object Producer:

  type Env = Finnhub.Finnhub with Db.Db with Http.Http with Clock

  private val producers: List[Producer] =
    val _producers =
      List(
        Ping,
        Time,
        Eval,
        Html,
        Substitute,
        Count,
        Stock,
        Weather(sys.env("DARK_SKY_API_KEY"))
      )
    new Help(_producers) :: _producers

  def init(): RIO[Env, Iterable[Unit]] =
    ZIO.foreach(producers)(_.init())

  def apply(m: Rx): URIO[Env, Iterable[Tx]] =
    ZIO.foldLeft(producers)(List.empty) {
      (txs, p) => p.apply(m).map(_.toList).map(_ ++ txs)
    }
