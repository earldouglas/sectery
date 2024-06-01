package sectery.irc

import javax.net.ssl.SSLSocketFactory
import org.pircbotx.Configuration
import org.pircbotx.PircBotX
import org.pircbotx.cap.SASLCapHandler
import org.pircbotx.delay.StaticDelay
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.JoinEvent
import org.pircbotx.hooks.events.MessageEvent
import org.pircbotx.hooks.types.GenericMessageEvent
import scala.jdk.CollectionConverters._
import sectery._
import sectery.domain.entities._
import sectery.domain.operations._
import sectery.effects._
import sectery.queue._
import zio.Queue
import zio.Unsafe
import zio.ZIO
import zio.ZLayer
import zio.json.DeriveJsonDecoder
import zio.json.DeriveJsonEncoder
import zio.stream.ZStream

class Bot(
    hostname: String,
    port: Int,
    channels: List[String],
    username: String,
    password: String,
    unsafeReceive: Rx => Unit
):

  private val messageDelayMs = 250

  private val pircBotX: PircBotX =
    new PircBotX(
      new Configuration.Builder()
        .setSocketFactory(SSLSocketFactory.getDefault())
        .addCapHandler(
          new SASLCapHandler(username, password)
        )
        .setName(username)
        .setRealName(username)
        .setLogin(username)
        .setNickservPassword(password)
        .addServer(hostname, port)
        .setAutoReconnect(true)
        .setMessageDelay(new StaticDelay(messageDelayMs))
        .addAutoJoinChannels(channels.asJava)
        .addListener(
          new ListenerAdapter {
            override def onGenericMessage(
                event: GenericMessageEvent
            ): Unit =
              if event.getUser().getNick() != username then
                event match
                  case e: MessageEvent =>
                    unsafeReceive(
                      Rx(
                        channel = e.getChannel().getName(),
                        nick = e.getUser().getNick(),
                        message = e.getMessage()
                      )
                    )
            override def onJoin(
                event: JoinEvent
            ): Unit =
              if event.getUser().getNick() != username then
                unsafeSend(
                  Tx(
                    channel = event.getChannel().getName(),
                    message = s"Hi, ${event.getUser().getNick()}!"
                  )
                )
          }
        )
        .buildConfiguration()
    )

  private def unsafeSend(tx: Tx): Unit =
    pircBotX.sendIRC().message(tx.channel, tx.message)

  def unsafeStart(): Unit =
    pircBotX.startBot()

object Bot:

  def apply(
      rabbitMqHostname: String,
      rabbitMqPort: Int,
      rabbitMqUsername: String,
      rabbitMqPassword: String,
      rabbitMqChannelSuffix: String,
      ircUsername: String,
      ircPassword: String,
      ircHostname: String,
      ircPort: Int,
      ircChannels: List[String]
  ): ZIO[Any, Throwable, Unit] =
    (
      for
        runtime <- ZIO.runtime

        received <- Queue.unbounded[Rx]

        unsafeReceive: (Rx => Unit) = { rx =>
          Unsafe
            .unsafe { implicit u: Unsafe =>
              runtime.unsafe
                .run(
                  received
                    .offer(rx)
                    .map(_ => ()) // Discard boolean
                )
                .getOrThrowFiberFailure()
            }
        }

        bot =
          new Bot(
            hostname = ircHostname,
            port = ircPort,
            channels = ircChannels,
            username = ircUsername,
            password = ircPassword,
            unsafeReceive = unsafeReceive
          )

        rabbitMQ =
          new RabbitMQ(
            hostname = rabbitMqHostname,
            port = rabbitMqPort,
            username = rabbitMqUsername,
            password = rabbitMqPassword
          )

        _ <-
          QueueDownstream
            .respondLoop()
            .provideLayer {

              val receive: QueueDownstream.ReceiveR =
                new Receive[[A] =>> ZIO[Any, Throwable, A]]:
                  override def name = "IRC"
                  override def apply() =
                    received.take

              val enqueue: QueueDownstream.EnqueueR =
                rabbitMQ.enqueue[Rx](s"inbox-${rabbitMqChannelSuffix}")(
                  using DeriveJsonEncoder.gen[Rx]
                )

              val send: QueueDownstream.SendR =
                new Send[[A] =>> ZIO[Any, Throwable, A]]:
                  override def name = "IRC"
                  override def apply(tx: Tx) =
                    ZIO.attempt(bot.unsafeSend(tx))

              val dequeue: QueueDownstream.DequeueR =
                rabbitMQ.dequeue[Tx](
                  s"outbox-${rabbitMqChannelSuffix}"
                )(using DeriveJsonDecoder.gen[Tx])

              val logger: QueueDownstream.LoggerR =
                new Logger:
                  override def debug(message: => String) =
                    ZIO.logDebug(message)
                  override def error(message: => String) =
                    ZIO.logError(message)

              ZLayer.succeed(logger) ++
                ZLayer.succeed(receive) ++
                ZLayer.succeed(enqueue) ++
                ZLayer.succeed(send) ++
                ZLayer.succeed(dequeue)
            }

        _ <- ZIO.logDebug(s"Starting IRC bot")
        _ <- ZIO.attemptBlocking(bot.unsafeStart())
      yield ()
    )
