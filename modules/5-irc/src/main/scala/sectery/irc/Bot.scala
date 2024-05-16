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
import sectery.MessageQueue
import sectery.Runtime.catchAndLog
import sectery.domain.entities._
import zio.Clock
import zio.Fiber
import zio.Unsafe
import zio.ZIO

object Bot:

  val messageDelayMs = 250

  type Env = MessageQueue & Clock

  def start(
      hostname: String,
      port: Int,
      channels: List[String],
      username: String,
      password: String
  ): ZIO[Env, Throwable, Fiber[Throwable, Any]] =
    for
      runtime <- ZIO.runtime
      inbox <- MessageQueue.inbox
      outbox <- MessageQueue.outbox
      bot = Bot(
        hostname = hostname,
        port = port,
        channels = channels,
        username = username,
        password = password,
        rx = (m: Rx) =>
          Unsafe.unsafe { implicit u: Unsafe =>
            runtime.unsafe
              .run {
                catchAndLog(
                  for
                    _ <- ZIO.logDebug(s"offering ${m} to inbox")
                    _ <- inbox.offer(Some(m))
                  yield ()
                )
              }
              .getOrThrowFiberFailure()
          },
        tx = (m: Tx) =>
          Unsafe.unsafe { implicit u: Unsafe =>
            runtime.unsafe
              .run {
                catchAndLog(
                  for
                    _ <- ZIO.logDebug(s"offering ${m} to outbox")
                    _ <- outbox.offer(Some(m))
                  yield ()
                )
              }
              .getOrThrowFiberFailure()
          }
      )
      botFiber <- ZIO.attemptBlocking(bot.startBot()).fork
      outboxFiber <- outbox.take
        .tap(tx => ZIO.logDebug(s"took ${tx} from outbox"))
        .tap(tx => ZIO.logDebug(s"sending ${tx} to IRC"))
        .mapZIO(tx =>
          ZIO
            .attempt(bot.send(tx))
        )
        .runDrain
        .fork
      fiber <- Fiber.joinAll(List(botFiber, outboxFiber)).fork
    yield fiber

class Bot(
    hostname: String,
    port: Int,
    username: String,
    password: String,
    channels: List[String],
    rx: Rx => Unit,
    tx: Tx => Unit
) extends PircBotX(
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
        .setMessageDelay(new StaticDelay(Bot.messageDelayMs))
        .addAutoJoinChannels(channels.asJava)
        .addListener(
          new ListenerAdapter {
            override def onGenericMessage(
                event: GenericMessageEvent
            ): Unit =
              if event.getUser().getNick() != username then
                event match
                  case e: MessageEvent =>
                    val m =
                      Rx(
                        channel = e.getChannel().getName(),
                        nick = e.getUser().getNick(),
                        message = e.getMessage()
                      )
                    rx(m)
            override def onJoin(
                event: JoinEvent
            ): Unit =
              if event.getUser().getNick() != username then
                val m =
                  Tx(
                    channel = event.getChannel().getName(),
                    message = s"Hi, ${event.getUser().getNick()}!"
                  )
                tx(m)
          }
        )
        .buildConfiguration()
    ):

  def send(tx: Tx): Unit =
    sendIRC.message(tx.channel, tx.message)
