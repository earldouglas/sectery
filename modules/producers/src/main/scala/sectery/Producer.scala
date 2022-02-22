package sectery

import org.slf4j.LoggerFactory
import sectery.Producer.Env
import sectery.producers._
import sectery.Runtime.catchAndLog
import sectery.Rx
import sectery.Tx
import zio.Clock
import zio.durationInt
import zio.Fiber
import zio.Schedule
import zio.ZHub
import zio.ZIO
import zio.ZQueue

/** A [[Producer]] takes an incoming message and produces any number of
  * responses based on implementation-specific logic.
  */
trait Producer:

  /** Documents what this producer responds to and how to use it.
    */
  def help(): Iterable[producers.Info]

  /** Runs any initialization (e.g. run DDL) needed.
    */
  def init(): ZIO[Producer.Env, Throwable, Unit] =
    ZIO.succeed(())

  /** Reads an incoming message, and produces any number of responses.
    */
  def apply(m: Rx): ZIO[Producer.Env, Throwable, Iterable[Tx]]

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
        Ascii,
        Morse,
        Hack
      )
    )

  def start(
      inbox: Queue[Rx],
      outbox: Queue[Tx]
  ): ZIO[Env, Throwable, Fiber[Throwable, Unit]] =
    for
      _ <- ZIO.foreach(producers)(_.init())
      autoquoteFiber <- Autoquote(outbox)
        .repeat(Schedule.spaced(5.minutes))
        .fork
      producerFiber <- {
        for
          rx <- inbox.take
          _ <- ZIO.collectAllPar {
            producers.map { producer =>
              for
                txs <- catchAndLog(producer(rx), List.empty[Tx])
                _ <- outbox.offer(txs)
              yield ()
            }
          }
        yield ()
      }.forever.fork
      fiber <- Fiber.joinAll(List(autoquoteFiber, producerFiber)).fork
    yield fiber
