package sectery.irc

import zio.ExitCode
import zio.ZIO
import zio.ZIOAppArgs
import zio.ZIOAppDefault
import zio.ZLayer
import zio.logging.backend.SLF4J

/** This is the main entry point for the module. Set your env vars, then
  * run the `main` method.
  */
object Main extends ZIOAppDefault:

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    zio.Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  val rabbitMqHostname: String = sys.env("RABBIT_MQ_HOSTNAME")
  val rabbitMqPort: Int = sys.env("RABBIT_MQ_PORT").toInt
  val rabbitMqUsername: String = sys.env("RABBIT_MQ_USERNAME")
  val rabbitMqPassword: String = sys.env("RABBIT_MQ_PASSWORD")

  val ircUsername: String = sys.env("IRC_USER")
  val ircPassword: String = sys.env("IRC_PASS")
  val ircHostname: String = sys.env("IRC_HOST")
  val ircPort: Int = sys.env("IRC_PORT").toInt
  val ircChannels: List[String] =
    sys.env("IRC_CHANNELS").split(",").map(_.trim).toList

  override def run: ZIO[Any, Throwable, ExitCode] =
    Bot
      .apply(
        rabbitMqHostname = rabbitMqHostname,
        rabbitMqPort = rabbitMqPort,
        rabbitMqUsername = rabbitMqUsername,
        rabbitMqPassword = rabbitMqPassword,
        rabbitMqChannelSuffix = "live",
        ircUsername = ircUsername,
        ircPassword = ircPassword,
        ircHostname = ircHostname,
        ircPort = ircPort,
        ircChannels = ircChannels
      )
      .map(_ => ExitCode.failure) // should never exit
