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
import sectery.Rx
import sectery.Tx
import zio.Clock
import zio.Fiber
import zio.Unsafe
import zio.ZIO

object Bot:

  val messageDelayMs = 250

  type Env = MessageQueue with Clock

  def start: ZIO[Env, Throwable, Fiber[Throwable, Any]] =
    for
      runtime <- ZIO.runtime
      inbox <- MessageQueue.inbox
      outbox <- MessageQueue.outbox
      bot = Bot(
        (m: Rx) =>
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
        (m: Tx) =>
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

class Bot(rx: Rx => Unit, tx: Tx => Unit)
    extends PircBotX(
      new Configuration.Builder()
        .setSocketFactory(SSLSocketFactory.getDefault())
        .addCapHandler(
          new SASLCapHandler(sys.env("IRC_USER"), sys.env("IRC_PASS"))
        )
        .setName(sys.env("IRC_USER"))
        .setRealName(sys.env("IRC_USER"))
        .setLogin(sys.env("IRC_USER"))
        .setNickservPassword(sys.env("IRC_PASS"))
        .addServer(sys.env("IRC_HOST"), sys.env("IRC_PORT").toInt)
        .setAutoReconnect(true)
        .setMessageDelay(new StaticDelay(Bot.messageDelayMs))
        .addAutoJoinChannels(
          sys
            .env("IRC_CHANNELS")
            .split(",")
            .map(_.trim)
            .toSeq
            .asJava
        )
        .addListener(
          new ListenerAdapter {
            override def onGenericMessage(
                event: GenericMessageEvent
            ): Unit =
              if event.getUser().getNick() != sys.env("IRC_USER") then
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
              if event.getUser().getNick() != sys.env("IRC_USER") then
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
