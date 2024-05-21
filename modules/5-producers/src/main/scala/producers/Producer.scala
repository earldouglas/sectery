package sectery.producers

import java.sql.Connection
import java.time.Instant
import java.util.concurrent.TimeUnit
import sectery.MessageQueue
import sectery.adaptors._
import sectery.control._
import sectery.domain.entities._
import sectery.domain.operations._
import sectery.effects._
import sectery.usecases._
import zio.Clock
import zio.Fiber
import zio.Schedule
import zio.ZIO
import zio.durationInt

class Producer(
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

  given autoquote: Autoquote[XIO] =
    grabQuoteSubstitute

  val getSetConfig =
    LiveConfig(unsafeGetConnection())

  def start: ZIO[MessageQueue, Nothing, Fiber[Throwable, Unit]] =
    for
      inbox <- MessageQueue.inbox
      outbox <- MessageQueue.outbox

      sendMessage =
        new SendMessage[XIO]:
          override def sendMessage(tx: Tx): XIO[Unit] =
            for
              _ <- ZIO.logDebug(s"offering ${tx} to outbox")
              _ <- outbox.offer(List(tx))
            yield ()

      autoquoteFiber <- {

        given _sendMessage: SendMessage[XIO] = sendMessage
        val announcers = new Announcers()

        announcers
          .announce()
          .repeat(Schedule.spaced(5.minutes))
          .fork
      }
      respondToMessage = {
        given _sendMessage: SendMessage[XIO] = sendMessage
        new RespondToMessage()
      }
      respondToMessageFiber <-
        inbox.take
          .tap(rx => ZIO.logDebug(s"took ${rx} from inbox"))
          .mapZIO { rx =>
            respondToMessage.receiveMessage(rx)
          }
          .runDrain
          .fork
      fiber <-
        Fiber
          .joinAll(
            List(
              autoquoteFiber,
              respondToMessageFiber
            )
          )
          .fork
    yield fiber
