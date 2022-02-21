package sectery

import com.amazonaws.regions.Regions
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.model.MessageAttributeValue
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.amazonaws.services.sqs.model.SendMessageBatchRequest
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry
import java.util.UUID
import scala.collection.mutable.Buffer
import scala.jdk.CollectionConverters._
import zio.json.JsonCodec
import zio.Task
import zio.ZIO

class SQSFifoQueue[A: JsonCodec](
    region: Regions,
    fifoQueueUrl: String,
    messageGroupId: String
) {

  private val codec = implicitly[JsonCodec[A]]

  private val sqs: AmazonSQS =
    AmazonSQSClientBuilder
      .standard()
      .withRegion(region)
      .build()

  private def toEntry(a: A): SendMessageBatchRequestEntry =
    val id = UUID.randomUUID().toString()
    val entry = new SendMessageBatchRequestEntry()
    entry.setId(id)
    entry.setMessageGroupId(messageGroupId)
    entry.setMessageDeduplicationId(id)
    entry.setMessageBody(codec.encodeJson(a, None).toString())
    entry

  def offer(as: Iterable[A]): Task[Boolean] =
    if (as.isEmpty) then ZIO.succeed(false)
    else
      val sendMessageBatchRequest: SendMessageBatchRequest =
        new SendMessageBatchRequest(
          fifoQueueUrl,
          as.map(toEntry).toList.asJava
        )
      ZIO
        .attempt(sqs.sendMessageBatch(sendMessageBatchRequest))
        .map(_ => true)

  private def decode(ms: List[Message]): List[A] =
    ms
      .map(_.getBody())
      .map(codec.decodeJson)
      .map(_.toOption)
      .flatten

  def take(): Task[List[A]] =
    val receiveMessageRequest: ReceiveMessageRequest =
      new ReceiveMessageRequest(fifoQueueUrl)
        .withWaitTimeSeconds(20)
        .withMaxNumberOfMessages(10)
    for
      messages <- ZIO
        .attempt(sqs.receiveMessage(receiveMessageRequest))
        .map(_.getMessages())
        .map(_.asScala.toList)
      _ <- ZIO.collectAll {
        messages.map { m =>
          ZIO.attempt(
            sqs.deleteMessage(fifoQueueUrl, m.getReceiptHandle())
          )
        }
      }
      as = decode(messages)
    yield as
}
