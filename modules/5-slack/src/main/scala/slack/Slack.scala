package sectery.slack

import com.slack.api.bolt.App
import com.slack.api.bolt.AppConfig
import com.slack.api.bolt.context.builtin.EventContext
import com.slack.api.bolt.response.Response
import com.slack.api.bolt.socket_mode.SocketModeApp
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.model.event.MessageBotEvent
import com.slack.api.model.event.MessageEvent
import scala.jdk.CollectionConverters._
import sectery._
import sectery.adaptors._
import sectery.domain.entities.Rx
import sectery.domain.entities.Tx
import sectery.domain.entities._
import sectery.domain.operations._
import sectery.effects._
import zio.Fiber
import zio.Queue
import zio.Unsafe
import zio.ZIO
import zio.ZLayer
import zio.json.DeriveJsonDecoder
import zio.json.DeriveJsonEncoder
import zio.json.JsonDecoder
import zio.json.JsonEncoder
import zio.stream.ZStream

// See https://slack.dev/java-slack-sdk/guides/web-api-basics
class Slack(
    botToken: String,
    appToken: String,
    unsafeReceive: Rx => Unit
):

  private val methods: MethodsClient =
    val slack: com.slack.api.Slack = com.slack.api.Slack.getInstance()
    slack.methods(botToken)

  private val socketModeApp: SocketModeApp =

    val app: App = new App(
      AppConfig.builder().singleTeamBotToken(botToken).build()
    )

    app.event(
      classOf[MessageEvent],
      (payload, ctx) => handleEvent(Event(payload.getEvent()), ctx)
    )

    app.event(
      classOf[MessageBotEvent],
      (payload, ctx) => handleEvent(Event(payload.getEvent()), ctx)
    )

    new SocketModeApp(appToken, app)

  private def handleEvent(event: Event, ctx: EventContext): Response =

    unsafeReceive(
      Rx(
        service = Slack.name,
        channel = event.channel,
        thread = Some(event.thread),
        nick = event.from,
        message = event.text
      )
    )

    ctx.ack()

  private def unsafeSend(tx: Tx): Unit =

    val request: ChatPostMessageRequest =
      val builder =
        ChatPostMessageRequest
          .builder()
          .channel(tx.channel)
          .text(tx.message)

      tx.thread.foreach(ts => builder.threadTs(ts))

      builder.build()

    methods.chatPostMessage(request)

    ()

  private def unsafeStart(): Unit =
    socketModeApp.start()

object Slack:

  val name: String = "slack"

  def apply(
      rabbitMqHostname: String,
      rabbitMqPort: Int,
      rabbitMqUsername: String,
      rabbitMqPassword: String,
      rabbitMqChannelSuffix: String,
      slackBotToken: String,
      slackAppToken: String
  ): ZIO[Any, Throwable, Unit] =
    (
      for
        runtime <- ZIO.runtime

        received <- Queue.unbounded[Rx]

        unsafeReceive: (Rx => Unit) = { rx =>
          Unsafe
            .unsafe { implicit u: Unsafe =>
              runtime.unsafe
                .run(
                  received
                    .offer(rx)
                    .map(_ => ()) // Discard boolean
                )
                .getOrThrowFiberFailure()
            }
        }

        slack =
          new Slack(
            botToken = slackBotToken,
            appToken = slackAppToken,
            unsafeReceive = unsafeReceive
          )

        rabbitMQ =
          new RabbitMQ(
            channels = None,
            hostname = rabbitMqHostname,
            port = rabbitMqPort,
            username = rabbitMqUsername,
            password = rabbitMqPassword
          )

        respondLoopFiber <-
          QueueDownstream
            .respondLoop()
            .provideLayer {

              val receive: QueueDownstream.ReceiveR =
                new Receive[[A] =>> ZIO[Any, Throwable, A]]:
                  override def name = Slack.name
                  override def apply() =
                    received.take

              val enqueue: QueueDownstream.EnqueueR =
                given encoder: JsonEncoder[Rx] =
                  DeriveJsonEncoder.gen[Rx]
                rabbitMQ.enqueue[Rx](
                  s"inbox-${Slack.name}-${rabbitMqChannelSuffix}"
                )

              val send: QueueDownstream.SendR =
                new Send[[A] =>> ZIO[Any, Throwable, A]]:
                  override def name = Slack.name
                  override def apply(tx: Tx) =
                    ZIO.attemptBlocking(slack.unsafeSend(tx))

              val dequeue: QueueDownstream.DequeueR =

                given hasChannel: HasChannel[Tx] =
                  new HasChannel:
                    override def getChannel(value: Tx) =
                      value.channel

                given decoder: JsonDecoder[Tx] =
                  DeriveJsonDecoder.gen[Tx]

                rabbitMQ.dequeue[Tx](
                  s"outbox-${Slack.name}-${rabbitMqChannelSuffix}"
                )

              val logger: QueueDownstream.LoggerR =
                new Logger:
                  override def debug(message: => String) =
                    ZIO.logDebug(message)
                  override def error(message: => String) =
                    ZIO.logError(message)

              ZLayer.succeed(logger) ++
                ZLayer.succeed(receive) ++
                ZLayer.succeed(enqueue) ++
                ZLayer.succeed(send) ++
                ZLayer.succeed(dequeue)
            }

        _ <- ZIO.logDebug(s"Starting Slack bot")
        startFiber <- ZIO.attemptBlocking(slack.unsafeStart()).fork
        _ <- Fiber.joinAll(List(respondLoopFiber, startFiber))
      yield ()
    )
