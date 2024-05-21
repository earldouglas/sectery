package sectery.effects

trait Eval[F[_]]:
  def eval(src: String): F[Either[String, String]]
