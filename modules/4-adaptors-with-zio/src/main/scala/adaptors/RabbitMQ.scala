package sectery.adaptors

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import java.nio.charset.StandardCharsets
import sectery._
import sectery.effects._
import zio.Chunk
import zio.ZIO
import zio.json.JsonDecoder
import zio.json.JsonEncoder
import zio.stream.ZStream

class RabbitMQ(
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

    connection.createChannel()

  def enqueue[A: JsonEncoder](
      queueName: String
  ): Enqueue[[A] =>> ZIO[Any, Throwable, A], A] =

    channel.queueDeclare(queueName, false, false, false, null)

    new Enqueue:
      override def name = queueName
      override def apply(value: A) =
        import zio.json.EncoderOps
        ZIO.attempt:
          val message: Array[Byte] =
            value.toJson.getBytes(StandardCharsets.UTF_8)
          channel.basicPublish("", queueName, null, message)

  def dequeue[A: HasChannel: JsonDecoder](
      queueName: String
  ): Dequeue[[A] =>> ZStream[Any, Throwable, A], A] =

    channel.queueDeclare(queueName, false, false, false, null)

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
                  val ch = summon[HasChannel[A]].getChannel(a)

                  val channelMatches =
                    channels match
                      case None => true
                      case Some(cs) =>
                        cs.contains(ch)

                  if channelMatches
                  then cb(ZIO.succeed(Chunk(a)))
                  else ()
              }
            }

          val cancel: CancelCallback =
            (consumerTag: String) =>
              cb(ZIO.fail(new Exception("cancelled")).mapError(Some(_)))

          channel.basicConsume(queueName, true, deliver, cancel)

          ()
        }
