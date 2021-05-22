package sectery

import org.pircbotx.Configuration
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.MessageEvent
import org.pircbotx.hooks.types.GenericMessageEvent
import org.pircbotx.PircBotX

class SecteryListener extends ListenerAdapter {
  override def onGenericMessage(event: GenericMessageEvent): Unit =
    event match
      case e: MessageEvent =>
        if e.getMessage() == "@ping" then
          e.respondChannel("pong")
}

object Sectery extends scala.App {
  import scala.collection.JavaConverters._
  val configuration: Configuration =
    new Configuration.Builder()
      .setName(sys.env("IRC_USER"))
      .setRealName(sys.env("IRC_USER"))
      .setLogin(sys.env("IRC_USER"))
      .setNickservPassword(sys.env("IRC_PASS"))
      .addServer(sys.env("IRC_HOST"))
      .addAutoJoinChannels(
        sys.env("IRC_CHANNELS")
          .split(",")
          .map(_.trim)
          .toIterable.asJava
      )
      .addListener(new SecteryListener())
      .buildConfiguration()
  val bot: PircBotX = new PircBotX(configuration)
  bot.startBot()
}
