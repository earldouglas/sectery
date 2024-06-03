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
import zio.ZIO
import zio.ZLayer
import zio.json.DeriveJsonDecoder
import zio.json.DeriveJsonEncoder
import zio.json.JsonDecoder
import zio.json.JsonEncoder
import zio.stream.ZStream

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

  type XIO[A] = ZIO[Any, Throwable, A]

  given monad: Monad[XIO] with
    override def pure[A](value: A): XIO[A] =
      ZIO.succeed(value)
    override def flatMap[A, B](fa: XIO[A])(f: A => XIO[B]): XIO[B] =
      fa.flatMap(f)

  given traversable: Traversable[XIO] with
    override def traverse[A, B](as: List[A])(
        f: A => XIO[B]
    ): XIO[List[B]] =
      ZIO.collectAll(as.map(f))

  given httpClient: HttpClient[XIO] =
    LiveHttpClient()

  given transactor: Transactor[XIO] with
    override def transact[A](k: Connection => A): XIO[A] =
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

  given now: Now[XIO] with
    override def now(): XIO[Instant] =
      Clock
        .currentTime(TimeUnit.MILLISECONDS)
        .map(Instant.ofEpochMilli(_))

  given btc: Btc[XIO] =
    LiveBtc()

  given counter: Counter[XIO] =
    LiveCounter(unsafeGetConnection())

  given eval: Eval[XIO] =
    LiveEval()

  given frinkiac: Frinkiac[XIO] =
    LiveFrinkiac()

  given getConfig: GetConfig[XIO] =
    getSetConfig

  given getWx: GetWx[XIO] =
    LiveGetWx(
      openWeatherMapApiKey = openWeatherMapApiKey,
      airNowApiKey = airNowApiKey
    )

  given grab: Grab[XIO] =
    grabQuoteSubstitute

  given hack: Hack[XIO] =
    LiveHack(unsafeGetConnection())

  given krypto: Krypto[XIO] =
    LiveKrypto(unsafeGetConnection())

  given lastMessage: LastMessage[XIO] =
    grabQuoteSubstitute

  given openAI: OpenAI[XIO] =
    LiveOpenAI(openAiApiKey)

  given points: Points[XIO] =
    LivePoints(unsafeGetConnection())

  given quote: Quote[XIO] =
    grabQuoteSubstitute

  given setConfig: SetConfig[XIO] =
    getSetConfig

  given stock: Stock[XIO] =
    LiveStock(finnhubApiToken)

  given tell: Tell[XIO] =
    LiveTell(unsafeGetConnection())

  val grabQuoteSubstitute =
    LiveGrabQuoteSubstitute(unsafeGetConnection())

  val getSetConfig =
    LiveConfig(unsafeGetConnection())

  def start: ZIO[Any, Nothing, Fiber[Throwable, Unit]] =

    val rabbitMQ: RabbitMQ =
      new RabbitMQ(
        channels = None,
        hostname = rabbitMqHostname,
        port = rabbitMqPort,
        username = rabbitMqUsername,
        password = rabbitMqPassword
      )

    def autoquoteFiber(outboxName: String) =
      given autoquote: Autoquote[XIO] =
        grabQuoteSubstitute

      given enqueueTx: Enqueue[XIO, Tx] =
        given encoder: JsonEncoder[Tx] =
          DeriveJsonEncoder.gen[Tx]
        rabbitMQ.enqueue[Tx](outboxName)

      new Announcers()
        .announce()
        .catchAllCause(cause => ZIO.logError(cause.prettyPrint))
        .fork

    def respondFiber(
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

      val enqueueTx: Enqueue[XIO, Tx] =
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

      autoquoteIrcFiber <- autoquoteFiber(outboxIrc)
      autoquoteSlackFiber <- autoquoteFiber(outboxSlack)

      respondIrcFiber <- respondFiber(inboxIrc, outboxIrc)
      respondSlackFiber <- respondFiber(inboxSlack, outboxSlack)

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
      databaseUrl: String,
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
