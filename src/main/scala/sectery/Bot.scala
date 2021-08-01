package sectery

import javax.net.ssl.SSLSocketFactory
import org.pircbotx.Configuration
import org.pircbotx.PircBotX
import org.pircbotx.cap.SASLCapHandler
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.JoinEvent
import org.pircbotx.hooks.events.MessageEvent
import org.pircbotx.hooks.types.GenericMessageEvent
import scala.collection.JavaConverters._
import zio.UIO
import zio.ZIO

object Bot extends Sender:

  var receive: Rx => Unit = { _ => () }

  private val config: Configuration =
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
      .addAutoJoinChannels(
        sys
          .env("IRC_CHANNELS")
          .split(",")
          .map(_.trim)
          .toIterable
          .asJava
      )
      .addListener(
        new ListenerAdapter {
          override def onGenericMessage(
              event: GenericMessageEvent
          ): Unit =
            event match
              case e: MessageEvent =>
                val m =
                  Rx(
                    channel = e.getChannel().getName(),
                    nick = e.getUser().getNick(),
                    message = e.getMessage()
                  )
                receive(m)
          override def onJoin(
              event: JoinEvent
          ): Unit =
            if (event.getUser().getNick() != sys.env("IRC_USER"))
              val m =
                Tx(
                  channel = event.getChannel().getName(),
                  message = s"Hi, ${event.getUser().getNick()}!"
                )
              unsafeSend(m)
        }
      )
      .buildConfiguration()

  private val bot: PircBotX =
    new PircBotX(config)

  override def send(m: Tx): UIO[Unit] =
    ZIO.succeed(unsafeSend(m))

  private def unsafeSend(m: Tx): Unit =
    bot.sendIRC.message(m.channel, m.message)

  def start(): UIO[Unit] =
    ZIO.succeedBlocking(bot.startBot())
