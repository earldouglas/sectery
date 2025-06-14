package sectery.producers

import java.sql.Connection
import java.time.Instant
import java.util.concurrent.TimeUnit
import sectery._
import sectery.adaptors._
import sectery.control._
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases._
import zio.Clock
import zio.Fiber
import zio.Schedule
import zio.Task
import zio.ZIO
import zio.ZLayer
import zio.durationInt
import zio.json.DeriveJsonDecoder
import zio.json.DeriveJsonEncoder
import zio.json.JsonDecoder
import zio.json.JsonEncoder

class Producers(
    rabbitMqHostname: String,
    rabbitMqPort: Int,
    rabbitMqUsername: String,
    rabbitMqPassword: String,
    rabbitMqChannelSuffix: String,
    unsafeGetConnection: () => Connection,
    openWeatherMapApiKey: String,
    airNowApiKey: String,
    finnhubApiToken: String,
    openAiApiKey: String
):

  given monad: Monad[Task] with
    override def pure[A](value: => A): Task[A] =
      ZIO.attempt(value)
    override def flatMap[A, B](fa: Task[A])(f: A => Task[B]): Task[B] =
      fa.flatMap(f)

  given traversable: Traversable[Task] with
    override def traverse[A, B](as: List[A])(
        f: A => Task[B]
    ): Task[List[B]] =
      ZIO.collectAll(as.map(f))

  given httpClient: HttpClient[Task] =
    LiveHttpClient()

  given transactor: Transactor[Task] with
    override def transact[A](k: Connection => A): Task[A] =
      val c = unsafeGetConnection()
      ZIO
        .attempt {
          c.setAutoCommit(false)
          val result: A = k(c)
          c.commit()
          c.close()
          result
        }
        .catchAllCause { cause =>
          for
            _ <- ZIO.logError("rolling back transaction")
            _ <- ZIO.logErrorCause(cause)
            _ = c.rollback()
            _ = c.close()
            f <- ZIO.fail(new Exception("rolled back transaction"))
          yield f
        }

  given now: Now[Task] with
    override def now(): Task[Instant] =
      Clock
        .currentTime(TimeUnit.MILLISECONDS)
        .map(Instant.ofEpochMilli(_))

  given btc: Btc[Task] =
    LiveBtc()

  given counter: Counter[Task] =
    LiveCounter(unsafeGetConnection())

  given eval: Eval[Task] =
    LiveEval()

  given frinkiac: Frinkiac[Task] =
    LiveFrinkiac()

  given getConfig: GetConfig[Task] =
    getSetConfig

  given getWx: GetWx[Task] =
    LiveGetWx(
      openWeatherMapApiKey = openWeatherMapApiKey,
      airNowApiKey = airNowApiKey
    )

  given grab: Grab[Task] =
    grabQuoteSubstitute

  given hack: Hack[Task] =
    LiveHack(unsafeGetConnection())

  given krypto: Krypto[Task] =
    LiveKrypto(unsafeGetConnection())

  given lastMessage: LastMessage[Task] =
    grabQuoteSubstitute

  given openAI: OpenAI[Task] =
    LiveOpenAI(openAiApiKey)

  given points: Points[Task] =
    LivePoints(unsafeGetConnection())

  given quote: Quote[Task] =
    grabQuoteSubstitute

  given setConfig: SetConfig[Task] =
    getSetConfig

  given stock: Stock[Task] =
    LiveStock(finnhubApiToken)

  given tell: Tell[Task] =
    LiveTell(unsafeGetConnection())

  val grabQuoteSubstitute =
    LiveGrabQuoteSubstitute(unsafeGetConnection())

  val getSetConfig =
    LiveConfig(unsafeGetConnection())

  def start: ZIO[Any, Nothing, Fiber[Throwable, Unit]] =

    def autoquoteFiber(rabbitMQ: RabbitMQ, outboxName: String) =
      given autoquote: Autoquote[Task] =
        grabQuoteSubstitute

      given enqueueTx: Enqueue[Task, Tx] =
        given encoder: JsonEncoder[Tx] =
          DeriveJsonEncoder.gen[Tx]
        rabbitMQ.enqueue[Tx](outboxName)

      new Announcers()
        .announce()
        .catchAllCause(cause => ZIO.logError(cause.prettyPrint))
        .fork

    def respondFiber(
        rabbitMQ: RabbitMQ,
        inboxName: String,
        outboxName: String
    ) =

      val dequeueRx: QueueUpstream.DequeueR =

        given hasChannel: HasChannel[Rx] =
          new HasChannel:
            override def getChannel(value: Rx) =
              value.channel

        given decoder: JsonDecoder[Rx] =
          DeriveJsonDecoder.gen[Rx]

        rabbitMQ.dequeue[Rx](inboxName)

      val enqueueTx: Enqueue[Task, Tx] =
        given encoder: JsonEncoder[Tx] =
          DeriveJsonEncoder.gen[Tx]
        rabbitMQ.enqueue[Tx](outboxName)

      val logger: QueueDownstream.LoggerR =
        new Logger:
          override def debug(message: => String) =
            ZIO.logDebug(message)
          override def error(message: => String) =
            ZIO.logError(message)

      QueueUpstream
        .respondLoop()
        .provide(
          ZLayer.succeed(logger) ++
            ZLayer.succeed(enqueueTx) ++
            ZLayer.succeed(dequeueRx) ++
            ZLayer.succeed(new Responders)
        )

    val inboxIrc = s"inbox-irc-${rabbitMqChannelSuffix}"
    val inboxSlack = s"inbox-slack-${rabbitMqChannelSuffix}"

    val outboxIrc = s"outbox-irc-${rabbitMqChannelSuffix}"
    val outboxSlack = s"outbox-slack-${rabbitMqChannelSuffix}"

    for

      rabbitMQ <-
        RabbitMQ(
          channels = None,
          hostname = rabbitMqHostname,
          port = rabbitMqPort,
          username = rabbitMqUsername,
          password = rabbitMqPassword
        )
          .retry(Schedule.spaced(1.seconds))
          .orDie

      autoquoteIrcFiber <- autoquoteFiber(rabbitMQ, outboxIrc)
      autoquoteSlackFiber <- autoquoteFiber(rabbitMQ, outboxSlack)

      respondIrcFiber <- respondFiber(rabbitMQ, inboxIrc, outboxIrc)
      respondSlackFiber <- respondFiber(
        rabbitMQ,
        inboxSlack,
        outboxSlack
      )

      fiber <-
        Fiber
          .joinAll(
            List(
              autoquoteIrcFiber,
              autoquoteSlackFiber,
              respondIrcFiber,
              respondSlackFiber
            )
          )
          .fork
    yield fiber

object Producers:

  def apply(
      openWeatherMapApiKey: String,
      airNowApiKey: String,
      finnhubApiToken: String,
      rabbitMqHostname: String,
      rabbitMqPort: Int,
      rabbitMqUsername: String,
      rabbitMqPassword: String,
      rabbitMqChannelSuffix: String,
      openAiApiKey: String,
      unsafeGetConnection: () => Connection
  ): ZIO[Any, Throwable, Nothing] = {
    for
      producerFiber <-
        new Producers(
          rabbitMqHostname = rabbitMqHostname,
          rabbitMqPort = rabbitMqPort,
          rabbitMqUsername = rabbitMqUsername,
          rabbitMqPassword = rabbitMqPassword,
          rabbitMqChannelSuffix = rabbitMqChannelSuffix,
          unsafeGetConnection = unsafeGetConnection,
          openWeatherMapApiKey = openWeatherMapApiKey,
          airNowApiKey = airNowApiKey,
          finnhubApiToken = finnhubApiToken,
          openAiApiKey = openAiApiKey
        ).start
      _ <- producerFiber.join
    yield ()
  }.forever
