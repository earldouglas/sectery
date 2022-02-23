package zio.sqs

import java.util.UUID
import zio.aws.core.AwsError
import zio.aws.sqs._
import zio.aws.sqs.model._
import zio.aws.sqs.model.primitives.Integer
import zio.aws.sqs.model.primitives.MessageAttributeName
import zio.json.JsonCodec
import zio.stream.ZStream
import zio.{RIO, ZIO}

object SqsStream {

  def apply(
      queueUrl: String,
      settings: SqsStreamSettings = SqsStreamSettings()
  ): ZStream[Sqs, Throwable, Message.ReadOnly] = {

    val request =
      ReceiveMessageRequest(
        queueUrl = queueUrl,
        attributeNames = Some(settings.attributeNames),
        messageAttributeNames = Some(
          settings.messageAttributeNames.map(
            MessageAttributeName.apply(_)
          )
        ),
        maxNumberOfMessages =
          Some(Integer(settings.maxNumberOfMessages)),
        visibilityTimeout =
          Some(Integer(settings.visibilityTimeout.getOrElse(30))),
        waitTimeSeconds =
          Some(Integer(settings.waitTimeSeconds.getOrElse(20)))
      )

    ZStream
      .repeatZIO(
        zio.aws.sqs.Sqs
          .receiveMessage(request)
          .mapError(_.toThrowable)
      )
      .map(_.messages.getOrElse(List.empty))
      .takeWhile(_.nonEmpty || !settings.stopWhenQueueEmpty)
      .mapConcat(identity)
      .mapZIO(msg =>
        ZIO
          .when(settings.autoDelete)(deleteMessage(queueUrl, msg))
          .as(msg)
      )
  }

  def deleteMessage(
      queueUrl: String,
      msg: Message.ReadOnly
  ): RIO[Sqs, Unit] =
    zio.aws.sqs.Sqs
      .deleteMessage(
        DeleteMessageRequest(queueUrl, msg.receiptHandle.getOrElse(""))
      )
      .mapError(_.toThrowable)

  private def codec[A: JsonCodec]: JsonCodec[A] =
    implicitly[JsonCodec[A]]

  private def toMessageBatchRequestEntry[A: JsonCodec](
      messageGroupId: String,
      body: A
  ): SendMessageBatchRequestEntry =
    val id = UUID.randomUUID().toString()
    SendMessageBatchRequestEntry(
      id = id,
      messageBody = codec.encodeJson(body, None).toString(),
      delaySeconds = None,
      messageAttributes = None,
      messageSystemAttributes = None,
      messageDeduplicationId = Some(id),
      messageGroupId = Some(messageGroupId)
    )

  def sendMessages[A: JsonCodec](
      queueUrl: String,
      messageGroupId: String,
      as: Iterable[A]
  ): ZIO[Sqs, AwsError, SendMessageBatchResponse.ReadOnly] =
    val request =
      SendMessageBatchRequest(
        queueUrl = queueUrl,
        entries =
          as.map(a => toMessageBatchRequestEntry(messageGroupId, a))
      )
    zio.aws.sqs.Sqs.sendMessageBatch(request)

  def receiveMessage(
      queueUrl: String,
      settings: SqsStreamSettings = SqsStreamSettings()
  ): ZIO[Sqs, Throwable, ReceiveMessageResponse.ReadOnly] =

    val request =
      ReceiveMessageRequest(
        queueUrl = queueUrl,
        attributeNames = Some(settings.attributeNames),
        messageAttributeNames = Some(
          settings.messageAttributeNames.map(
            MessageAttributeName.apply(_)
          )
        ),
        maxNumberOfMessages =
          Some(Integer(settings.maxNumberOfMessages)),
        visibilityTimeout =
          Some(Integer(settings.visibilityTimeout.getOrElse(30))),
        waitTimeSeconds =
          Some(Integer(settings.waitTimeSeconds.getOrElse(20)))
      )

    zio.aws.sqs.Sqs
      .receiveMessage(request)
      .mapError(_.toThrowable)
}
