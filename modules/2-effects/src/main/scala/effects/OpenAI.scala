package sectery.effects

trait OpenAI[F[_]]:
  def complete(prompt: String): F[List[String]]
