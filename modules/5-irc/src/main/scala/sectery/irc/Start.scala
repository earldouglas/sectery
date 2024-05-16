package sectery.irc

import sectery.RabbitMQ
import sectery.Runtime.catchAndLog
import zio.Clock
import zio.ZIO
import zio.ZLayer

object Start:

  def apply(
      rabbitMqHostname: String,
      rabbitMqPort: Int,
      rabbitMqUsername: String,
      rabbitMqPassword: String,
      ircHostname: String,
      ircPort: Int,
      ircChannels: List[String],
      ircUsername: String,
      ircPassword: String,
      channelSuffix: String
  ): ZIO[Any, Nothing, Nothing] =
    catchAndLog {
      for
        botFiber <-
          Bot.start(
            hostname = ircHostname,
            port = ircPort,
            channels = ircChannels,
            username = ircUsername,
            password = ircPassword
          )
        _ <- botFiber.join
      yield ()
    }
      .provide(
        ZLayer.succeed(
          Clock.ClockLive
        ) ++
          RabbitMQ(
            channelSuffix = channelSuffix,
            hostname = rabbitMqHostname,
            port = rabbitMqPort,
            username = rabbitMqUsername,
            password = rabbitMqPassword
          )
      )
      .forever
