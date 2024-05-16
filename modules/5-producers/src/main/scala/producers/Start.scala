package sectery.producers

import java.sql.Connection
import sectery.RabbitMQ
import sectery.Runtime.catchAndLog
import zio.Clock
import zio.ZIO
import zio.ZLayer

object Start:

  def apply(
      databaseUrl: String,
      openWeatherMapApiKey: String,
      airNowApiKey: String,
      finnhubApiToken: String,
      rabbitMqHostname: String,
      rabbitMqPort: Int,
      rabbitMqUsername: String,
      rabbitMqPassword: String,
      openAiApiKey: String,
      unsafeGetConnection: () => Connection,
      channelSuffix: String
  ): ZIO[Any, Throwable, Nothing] =
    catchAndLog {
      for
        producerFiber <-
          new Producer(
            unsafeGetConnection = unsafeGetConnection,
            openWeatherMapApiKey = openWeatherMapApiKey,
            airNowApiKey = airNowApiKey,
            finnhubApiToken = finnhubApiToken,
            openAiApiKey = openAiApiKey
          ).start
        _ <- producerFiber.join
      yield ()
    }
      .provide(
        ZLayer.succeed(
          Clock.ClockLive
        ) ++ RabbitMQ(
          channelSuffix = channelSuffix,
          hostname = rabbitMqHostname,
          port = rabbitMqPort,
          username = rabbitMqUsername,
          password = rabbitMqPassword
        )
      )
      .forever
