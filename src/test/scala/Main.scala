package sectery.test

import com.dimafeng.testcontainers.MariaDBContainer
import com.dimafeng.testcontainers.RabbitMQContainer
import java.sql.DriverManager
import sectery.Runtime.catchAndLog
import java.sql.Connection
import zio.ExitCode
import zio.ZIO
import zio.Fiber
import zio.ZIOAppDefault

object Main extends ZIOAppDefault:

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

  override def run: ZIO[Any, Nothing, ExitCode] =
    catchAndLog {

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

      for
        producersFiber <-
          sectery.producers
            .Start(
              databaseUrl = mariaDbContainer.jdbcUrl,
              openWeatherMapApiKey = openWeatherMapApiKey,
              airNowApiKey = airNowApiKey,
              finnhubApiToken = finnhubApiToken,
              rabbitMqHostname = rabbitMqHostname,
              rabbitMqPort = rabbitMqPort,
              rabbitMqUsername = rabbitMqUsername,
              rabbitMqPassword = rabbitMqPassword,
              openAiApiKey = openAiApiKey,
              unsafeGetConnection = unsafeGetConnection,
              channelSuffix = "test"
            )
            .fork
        ircFiber <-
          sectery.irc
            .Start(
              rabbitMqHostname = rabbitMqHostname,
              rabbitMqPort = rabbitMqPort,
              rabbitMqUsername = rabbitMqUsername,
              rabbitMqPassword = rabbitMqPassword,
              ircHostname = ircHostname,
              ircPort = ircPort,
              ircChannels = ircChannels,
              ircUsername = ircUsername,
              ircPassword = ircPassword,
              channelSuffix = "test"
            )
            .fork
        _ <- Fiber.joinAll(List(producersFiber, ircFiber))
      yield ()
    }.map(_ => ExitCode.failure) // should never exit
