package sectery.adaptors

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters._
import sectery._
import sectery.effects._
import zio.Chunk
import zio.ZIO
import zio.json.JsonDecoder
import zio.json.JsonEncoder
import zio.stream.ZStream

class RabbitMQ(
    service: Option[String],
    channels: Option[List[String]],
    hostname: String,
    port: Int,
    username: String,
    password: String
):

  private val channel: Channel =

    val connectionFactory: ConnectionFactory =
      new ConnectionFactory()

    connectionFactory.setHost(hostname)
    connectionFactory.setPort(port)
    connectionFactory.setUsername(username)
    connectionFactory.setPassword(password)
    connectionFactory.setAutomaticRecoveryEnabled(true)
    connectionFactory.setNetworkRecoveryInterval(1000)

    val connection: Connection =
      connectionFactory.newConnection()

    val channel: Channel =
      connection.createChannel()

    channel.basicQos(1)

    channel

  def enqueue[A: JsonEncoder](
      queueName: String
  ): Enqueue[[A] =>> ZIO[Any, Throwable, A], A] =

    channel.queueDeclare(
      queueName,
      true, // durable
      false,
      false, // not exclusive, not auto-delete,
      Map(
        "x-queue-type" -> "stream",
        "x-max-age" -> "1h"
      ).asJava
    )

    new Enqueue:
      override def name = queueName
      override def apply(value: A) =
        import zio.json.EncoderOps
        ZIO.attempt:
          val message: Array[Byte] =
            value.toJson.getBytes(StandardCharsets.UTF_8)
          channel.basicPublish("", queueName, null, message)

  def dequeue[A: HasService: HasChannel: JsonDecoder](
      queueName: String
  ): Dequeue[[A] =>> ZStream[Any, Throwable, A], A] =

    channel.queueDeclare(
      queueName,
      true, // durable
      false,
      false, // not exclusive, not auto-delete,
      Map(
        "x-queue-type" -> "stream",
        "x-max-age" -> "1h"
      ).asJava
    )

    new Dequeue:
      override def name = queueName
      override def apply() =
        import zio.json.DecoderOps
        ZStream.async[Any, Throwable, A] { cb =>

          val deliver: DeliverCallback =
            (consumerTag: String, message: Delivery) => {
              val messageBody: String =
                new String(message.getBody, "UTF-8")
              messageBody.fromJson[A] match {

                case Left(e) =>
                  cb(ZIO.fail(new Exception(e)).mapError(Some(_)))

                case Right(a) =>
                  val serviceMatches =
                    service match
                      case None => true
                      case Some(s) =>
                        s == summon[HasService[A]].getService(a)

                  val ch = summon[HasChannel[A]].getChannel(a)

                  val channelMatches =
                    channels match
                      case None => true
                      case Some(cs) =>
                        cs.contains(ch)

                  channel.basicAck(
                    message.getEnvelope().getDeliveryTag(),
                    true
                  )

                  if serviceMatches && channelMatches
                  then cb(ZIO.succeed(Chunk(a)))
                  else ()
              }
            }

          val cancel: CancelCallback =
            (consumerTag: String) =>
              cb(ZIO.fail(new Exception("cancelled")).mapError(Some(_)))

          channel.basicConsume(
            queueName,
            false,
            Map(
              "x-stream-offset" -> new java.util.Date()
            ).asJava,
            deliver,
            cancel
          )

          ()
        }
