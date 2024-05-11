package sectery

import sectery.Runtime.catchAndLog
import sectery.producers._
import zio.Clock
import zio.Fiber
import zio.Schedule
import zio.ZIO
import zio.durationInt
import zio.openai.Completions

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

  type Env = MessageQueue
    with Db.Db
    with Http.Http
    with Clock
    with Completions

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
          openWeatherMapApiKey = sys.env("OPEN_WEATHER_MAP_API_KEY"),
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
        Hack,
        Points,
        OpenAI
      )
    )

  def start: ZIO[Env, Throwable, Fiber[Throwable, Unit]] =
    for
      _ <- ZIO.foreach(producers)(_.init())
      inbox <- MessageQueue.inbox
      outbox <- MessageQueue.outbox
      autoquoteFiber <- Autoquote(outbox)
        .repeat(Schedule.spaced(5.minutes))
        .fork
      producerFiber <-
        inbox.take
          .tap(rx => ZIO.logDebug(s"took ${rx} from inbox"))
          .mapZIO { rx =>
            ZIO.collectAllPar {
              producers.map { producer =>
                for
                  txs <- catchAndLog(producer(rx), List.empty[Tx])
                  _ <-
                    if (!txs.isEmpty)
                    then ZIO.logDebug(s"offering ${txs} to outbox")
                    else ZIO.succeed(())
                  _ <- outbox.offer(txs)
                yield ()
              }
            }
          }
          .runDrain
          .fork
      fiber <- Fiber.joinAll(List(autoquoteFiber, producerFiber)).fork
    yield fiber
