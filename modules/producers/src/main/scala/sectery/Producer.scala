package sectery

import org.slf4j.LoggerFactory
import org.slf4j.LoggerFactory
import sectery.Rx
import sectery.Tx
import sectery.Producer.Env
import sectery.producers._
import zio.Clock
import zio.durationInt
import zio.Fiber
import zio.Hub
import zio.Queue
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

  private def subscribe(
      producer: Producer,
      inbox: Hub[Rx],
      outbox: Queue[Tx]
  ): ZIO[Producer.Env, Nothing, Fiber[Nothing, Any]] =
    for
      p <- zio.Promise.make[Nothing, Unit]
      fiber <-
        inbox.subscribe.use { case q =>
          {
            for
              _ <- p.succeed(())
              rx <- q.take
              txs <- producer(rx).catchAllCause { cause =>
                LoggerFactory
                  .getLogger(this.getClass())
                  .error(cause.prettyPrint)
                ZIO.succeed(List.empty[Tx])
              }
              _ <- outbox.offerAll(txs)
            yield ()
          }.forever
        }.fork
      _ <- p.await
    yield fiber

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
      inbox: Hub[Rx],
      outbox: Queue[Tx]
  ): ZIO[Env, Throwable, Fiber[Nothing, Any]] =
    for
      _ <- ZIO.foreach(producers)(_.init())
      autoquoteFiber <- Autoquote(outbox)
        .repeat(Schedule.spaced(5.minutes))
        .fork
      fibers <- ZIO.collectAll(
        producers.map { p =>
          subscribe(p, inbox, outbox)
        }
      )
      fiber <- Fiber.joinAll(autoquoteFiber :: fibers).fork
    yield fiber
