package sectery.slack

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

  val slackBotToken: String = sys.env("SLACK_BOT_TOKEN")
  val slackAppToken: String = sys.env("SLACK_APP_TOKEN")

  override def run: ZIO[Any, Throwable, ExitCode] =
    Slack
      .apply(
        rabbitMqHostname = rabbitMqHostname,
        rabbitMqPort = rabbitMqPort,
        rabbitMqUsername = rabbitMqUsername,
        rabbitMqPassword = rabbitMqPassword,
        rabbitMqChannelSuffix = "live",
        slackBotToken = slackBotToken,
        slackAppToken = slackAppToken
      )
      .map(_ => ExitCode.failure) // should never exit
