package sectery.test

import com.dimafeng.testcontainers.MariaDBContainer
import com.dimafeng.testcontainers.RabbitMQContainer
import java.sql.Connection
import java.sql.DriverManager
import sectery.irc.Bot
import zio.ExitCode
import zio.Fiber
import zio.Runtime
import zio.logging.backend.SLF4J
import zio.ZIO
import zio.ZIOAppDefault
import zio.ZLayer
import zio.ZIOAppArgs

object Main extends ZIOAppDefault:

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  val openWeatherMapApiKey: String = sys.env("OPEN_WEATHER_MAP_API_KEY")
  val airNowApiKey: String = sys.env("AIRNOW_API_KEY")
  val finnhubApiToken: String = sys.env("FINNHUB_API_TOKEN")
  val openAiApiKey: String = sys.env("OPENAI_APIKEY")

  val ircUsername: String = sys.env("IRC_USER")
  val ircPassword: String = sys.env("IRC_PASS")
  val ircHostname: String = sys.env("IRC_HOST")
  val ircPort: Int = sys.env("IRC_PORT").toInt
  val ircChannels: List[String] =
    sys.env("IRC_CHANNELS").split(",").map(_.trim).toList

  val slackBotToken: String = sys.env("SLACK_BOT_TOKEN")
  val slackAppToken: String = sys.env("SLACK_APP_TOKEN")

  override def run: ZIO[Any, Any, ExitCode] =
    val mariaDbContainer = MariaDBContainer.Def().createContainer()
    mariaDbContainer.start()

    val unsafeGetConnection: () => Connection =
      () =>
        DriverManager.getConnection(
          mariaDbContainer.jdbcUrl,
          mariaDbContainer.dbUsername,
          mariaDbContainer.dbPassword
        )

    val rabbitMqContainer = RabbitMQContainer.Def().createContainer()
    rabbitMqContainer.start()

    val rabbitMqHostname: String = rabbitMqContainer.host
    val rabbitMqPort: Int = rabbitMqContainer.amqpPort
    val rabbitMqUsername: String = rabbitMqContainer.adminUsername
    val rabbitMqPassword: String = rabbitMqContainer.adminPassword

    val start =
      for
        _ <- ZIO.logDebug(s"Launching Sectery in test mode")
        producersFiber <-
          sectery.producers.Producers
            .apply(
              databaseUrl = mariaDbContainer.jdbcUrl,
              openWeatherMapApiKey = openWeatherMapApiKey,
              airNowApiKey = airNowApiKey,
              finnhubApiToken = finnhubApiToken,
              rabbitMqHostname = rabbitMqHostname,
              rabbitMqPort = rabbitMqPort,
              rabbitMqUsername = rabbitMqUsername,
              rabbitMqPassword = rabbitMqPassword,
              rabbitMqChannelSuffix = "test",
              openAiApiKey = openAiApiKey,
              unsafeGetConnection = unsafeGetConnection
            )
            .fork
        ircFiber <-
          sectery.irc.Bot
            .apply(
              rabbitMqHostname = rabbitMqHostname,
              rabbitMqPort = rabbitMqPort,
              rabbitMqUsername = rabbitMqUsername,
              rabbitMqPassword = rabbitMqPassword,
              rabbitMqChannelSuffix = "test",
              ircUsername = ircUsername,
              ircPassword = ircPassword,
              ircHostname = ircHostname,
              ircPort = ircPort,
              ircChannels = ircChannels
            )
            .fork
        slackFiber <-
          sectery.slack.Slack
            .apply(
              rabbitMqHostname = rabbitMqHostname,
              rabbitMqPort = rabbitMqPort,
              rabbitMqUsername = rabbitMqUsername,
              rabbitMqPassword = rabbitMqPassword,
              rabbitMqChannelSuffix = "test",
              slackBotToken = slackBotToken,
              slackAppToken = slackAppToken
            )
            .fork
        _ <- Fiber.joinAll(List(producersFiber, ircFiber, slackFiber))
      yield ()

    start
      .map(_ => ExitCode.failure) // should never exit
