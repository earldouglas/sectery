package sectery

import org.slf4j.LoggerFactory
import sectery.producers._
import zio.Clock
import zio.RIO
import zio.UIO
import zio.URIO
import zio.ZIO

case class Info(name: String, usage: String)

trait Producer:

  /** Information about what this producer responds to and how to use
    * it.
    */
  def help(): Iterable[Info]

  /** Run any initialization (e.g. run DDL) needed.
    */
  def init(): RIO[Producer.Env, Unit] =
    ZIO.succeed(())

  /** Reads an incoming message, and produces any number of responses.
    */
  def apply(m: Rx): RIO[Producer.Env, Iterable[Tx]]

class Help(producers: List[Producer]) extends Producer:

  private val usage = """^@help\s+(.+)\s*$""".r

  private val helpMessage: String =
    s"""${producers
      .flatMap(p => p.help().map(_.name))
      .sorted
      .mkString(", ")}"""

  private val usageMap: Map[String, String] =
    producers.flatMap(_.help().map(i => i.name -> i.usage)).toMap

  override def help(): Iterable[Info] =
    None

  override def apply(m: Rx): UIO[Iterable[Tx]] =
    ZIO.succeed {
      m match
        case Rx(channel, _, "@help") =>
          Some(Tx(m.channel, helpMessage))
        case Rx(channel, _, usage(name)) =>
          usageMap.get(name) match
            case Some(usage) => Some(Tx(channel, s"Usage: ${usage}"))
            case None =>
              Some(Tx(channel, s"I don't know anything about ${name}"))
        case _ =>
          None
    }

object Help {
  def apply(producers: List[Producer]): List[Producer] =
    new Help(producers) :: producers
}

object Producer:

  type Env = Db.Db with Http.Http with Clock

  private val producers: List[Producer] =
    Help(
      List(
        Ping,
        Time,
        Eval,
        Html,
        Substitute,
        Count,
        Stock(sys.env("FINNHUB_API_TOKEN")),
        Weather(
          darkSkyApiKey = sys.env("DARK_SKY_API_KEY"),
          airNowApiKey = sys.env("AIRNOW_API_KEY")
        ),
        Btc,
        Version,
        Tell,
        LastMessage,
        Grab,
        Config,
        Frinkiac,
        Blink,
        Ascii
      )
    )

  def init(): RIO[Env, Iterable[Unit]] =
    ZIO.foreach(producers)(_.init())

  def apply(m: Rx): URIO[Env, Iterable[Tx]] =
    ZIO.foldLeft(producers)(List.empty) { (txs, p) =>
      p.apply(m)
        .catchAllCause { cause =>
          LoggerFactory
            .getLogger(this.getClass())
            .error(cause.prettyPrint)
          ZIO.succeed(None)
        }
        .map(_.iterator.to(List))
        .map(_ ++ txs)
    }
