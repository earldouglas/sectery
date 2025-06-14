package sectery.producers

import java.sql.Connection
import java.sql.DriverManager
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

  val databaseUrl: String = sys.env("DATABASE_URL")
  val openWeatherMapApiKey: String = sys.env("OPEN_WEATHER_MAP_API_KEY")
  val airNowApiKey: String = sys.env("AIRNOW_API_KEY")
  val finnhubApiToken: String = sys.env("FINNHUB_API_TOKEN")

  val rabbitMqHostname: String = sys.env("RABBIT_MQ_HOSTNAME")
  val rabbitMqPort: Int = sys.env("RABBIT_MQ_PORT").toInt
  val rabbitMqUsername: String = sys.env("RABBIT_MQ_USERNAME")
  val rabbitMqPassword: String = sys.env("RABBIT_MQ_PASSWORD")

  val openAiApiKey: String = sys.env("OPENAI_APIKEY")

  val unsafeGetConnection: () => Connection =
    () => DriverManager.getConnection(databaseUrl)

  override def run: ZIO[Any, Throwable, ExitCode] =
    Producers
      .apply(
        openWeatherMapApiKey = openWeatherMapApiKey,
        airNowApiKey = airNowApiKey,
        finnhubApiToken = finnhubApiToken,
        rabbitMqHostname = rabbitMqHostname,
        rabbitMqPort = rabbitMqPort,
        rabbitMqUsername = rabbitMqUsername,
        rabbitMqPassword = rabbitMqPassword,
        rabbitMqChannelSuffix = "live",
        openAiApiKey = openAiApiKey,
        unsafeGetConnection = unsafeGetConnection
      )
      .map(_ => ExitCode.failure) // should never exit
