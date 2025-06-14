package sectery.adaptors

import java.net.URI
import sectery._
import sectery.control.Monad
import sectery.control.Monad._
import sectery.effects.HttpClient.Response
import sectery.effects._
import zio.json._

object LiveOpenAI:

  def apply[F[_]: HttpClient: Monad](
      openAiApiKey: String
  ): OpenAI[F] =
    new OpenAI:

      case class Message(
          role: String,
          content: String
      )

      object Message:
        implicit val encoder: JsonEncoder[Message] =
          DeriveJsonEncoder.gen[Message]
        implicit val decoder: JsonDecoder[Message] =
          DeriveJsonDecoder.gen[Message]

      case class CompletionsRequest(
          model: String,
          max_tokens: Int,
          messages: List[Message],
          temperature: Float
      )

      object CompletionsRequest:
        implicit val encoder: JsonEncoder[CompletionsRequest] =
          DeriveJsonEncoder.gen[CompletionsRequest]

      case class Choice(
          message: Message
      )

      object Choice:
        implicit val decoder: JsonDecoder[Choice] =
          DeriveJsonDecoder.gen[Choice]

      case class CompletionsResponse(
          model: String,
          choices: List[Choice]
      )

      object CompletionsResponse:
        implicit val decoder: JsonDecoder[CompletionsResponse] =
          DeriveJsonDecoder.gen[CompletionsResponse]

      override def complete(prompt: String): F[List[String]] =
        summon[HttpClient[F]]
          .request(
            method = "GET",
            url = new URI("https://api.openai.com/v1/chat/completions")
              .toURL(),
            headers = Map(
              "Content-Type" -> "application/json",
              "Authorization" -> s"Bearer ${openAiApiKey}"
            ),
            body = Some(
              CompletionsRequest(
                model = "gpt-4o",
                max_tokens = 64,
                messages = List(
                  Message(
                    role = "system",
                    content = "You are a helpful assistant."
                  ),
                  Message(
                    role = "user",
                    content = "Do not acknowledge my request."
                  ),
                  Message(
                    role = "user",
                    content = prompt
                  )
                ),
                temperature = 0.7f
              ).toJson
            )
          )
          .map:
            case Response(200, _, body) =>
              body.fromJson[CompletionsResponse] match
                case Right(cr) => cr.choices.map(_.message.content)
                case Left(_)   => Nil
            case _ => Nil
