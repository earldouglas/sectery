package sectery

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import java.nio.charset.StandardCharsets
import zio.Chunk
import zio.Task
import zio.ZIO
import zio.ZLayer
import zio.json.DeriveJsonCodec
import zio.json.JsonCodec
import zio.stream.ZStream

class RabbitMQ[A](
    val channel: Channel,
    val queueName: String
)(implicit val jsonCodec: JsonCodec[A])
    extends Queue[A]:

  override def offer(as: Iterable[A]): Task[Unit] =
    import zio.json.EncoderOps
    ZIO.attempt:
      as.foreach { a =>
        val message: Array[Byte] =
          a.toJson.getBytes(StandardCharsets.UTF_8)
        channel.basicPublish("", queueName, null, message)
      }

  override val take: ZStream[Any, Throwable, A] =
    import zio.json.DecoderOps
    ZStream.async[Any, Throwable, A] { cb =>

      val deliver: DeliverCallback =
        (consumerTag: String, message: Delivery) => {
          val messageBody: String =
            new String(message.getBody, "UTF-8")
          messageBody.fromJson[A] match {
            case Left(e) =>
              cb(ZIO.fail(new Exception(e)).mapError(Some(_)))
            case Right(a) => cb(ZIO.succeed(Chunk(a)))
          }
        }

      val cancel: CancelCallback =
        (consumerTag: String) =>
          cb(ZIO.fail(new Exception("cancelled")).mapError(Some(_)))

      channel.basicConsume(queueName, true, deliver, cancel)

      ()
    }

object RabbitMQ:

  lazy val live: ZLayer[Any, Nothing, MessageQueue] = RabbitMQ("live")

  def apply(channelSuffix: String): ZLayer[Any, Nothing, MessageQueue] =
    ZLayer.succeed:
      new MessageQueue:

        val hostname: String = sys.env("RABBIT_MQ_HOSTNAME")
        val port: Int = sys.env("RABBIT_MQ_PORT").toInt
        val username: String = sys.env("RABBIT_MQ_USERNAME")
        val password: String = sys.env("RABBIT_MQ_PASSWORD")

        val inboxName: String = s"inbox-${channelSuffix}"
        val outboxName: String = s"outbox-${channelSuffix}"

        val connectionFactory: ConnectionFactory =
          new ConnectionFactory()
        connectionFactory.setHost(hostname)
        connectionFactory.setPort(port)
        connectionFactory.setUsername(username)
        connectionFactory.setPassword(password)
        val connection: Connection = connectionFactory.newConnection()

        val channel: Channel = connection.createChannel()
        channel.queueDeclare(inboxName, false, false, false, null)
        channel.queueDeclare(outboxName, false, false, false, null)

        override def inbox: Queue[Rx] =
          new RabbitMQ[Rx](channel, inboxName)(DeriveJsonCodec.gen[Rx])

        override def outbox: Queue[Tx] =
          new RabbitMQ[Tx](channel, outboxName)(DeriveJsonCodec.gen[Tx])
