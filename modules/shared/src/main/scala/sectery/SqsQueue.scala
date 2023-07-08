package sectery

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import zio.Chunk
import zio.Task
import zio.ZIO
import zio.ZLayer
import zio.aws.core.config.AwsConfig
import zio.aws.core.config.CommonAwsConfig
import zio.aws.netty.NettyHttpClient
import zio.aws.sqs.Sqs
import zio.json.JsonCodec
import zio.sqs.SqsStream
import zio.sqs.SqsStreamSettings
import zio.stream.ZStream

class SqsQueue[A: JsonCodec](
    region: Region,
    queueUrl: String,
    messageGroupId: String
) extends Queue[A]:

  private val codec = implicitly[JsonCodec[A]]
  private val sqsStreamSettings = SqsStreamSettings()

  private val env: ZLayer[Any, Throwable, Sqs] =
    NettyHttpClient.default ++
      ZLayer.succeed(
        CommonAwsConfig(
          region = Some(region),
          credentialsProvider = DefaultCredentialsProvider.create(),
          endpointOverride = None,
          commonClientConfig = None
        )
      ) >>>
      AwsConfig.configured() >>>
      Sqs.live

  override def offer(as: Iterable[A]): Task[Boolean] =
    if (as.isEmpty) then ZIO.succeed(true)
    else
      for
        response <- SqsStream
          .sendMessages(
            queueUrl = queueUrl,
            messageGroupId = messageGroupId,
            as = as
          )
          .mapError(_.toThrowable)
          .provide(env)
        failed <- response.getFailed
      yield failed.isEmpty

  override val take: ZStream[Any, Throwable, A] =
    SqsStream(
      queueUrl = queueUrl,
      settings = sqsStreamSettings
    )
      .mapZIO(
        _.getBody.map(codec.decodeJson).mapError(_.toThrowable).map {
          case Left(e)  => Chunk.empty
          case Right(a) => Chunk(a)
        }
      )
      .flattenChunks
      .provideLayer(env)
