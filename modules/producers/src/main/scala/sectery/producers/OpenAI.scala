package sectery.producers

import sectery.Producer
import sectery.Rx
import sectery.Tx
import zio.Chunk
import zio.ZIO

import zio.openai._
import zio.openai.model.CreateCompletionRequest.MaxTokens
import zio.openai.model.CreateCompletionRequest.Model
import zio.openai.model.CreateCompletionRequest.Model.Models
import zio.openai.model.CreateCompletionRequest.Prompt
import zio.openai.model.Temperature

import zio.openai.model.OpenAIFailure

object OpenAI extends Producer:

  private def complete(
      prompt: String
  ): ZIO[Completions, OpenAIFailure, Chunk[String]] =
    for {
      result <- Completions.createCompletion(
        model = Model.Predefined(Models.`Gpt-3.5-turbo-instruct`),
        prompt = Prompt.String(prompt),
        temperature = Temperature(0.6),
        maxTokens = MaxTokens(256)
      )
    } yield result.choices.map(_.text)

  private val ai = """^@ai\s+(.+)$""".r

  override def help(): Iterable[Info] =
    Some(Info("@ai", "@ai <prompt>"))

  override def apply(m: Rx): ZIO[Completions, Throwable, Iterable[Tx]] =
    m match
      case Rx(c, _, ai(prompt)) =>
        complete(prompt)
          .mapError(f => new Exception(s"OpenAIFailure: ${f}"))
          .map(chunk => chunk.toList.map(text => Tx(c, text)))
      case _ =>
        ZIO.succeed(None)
