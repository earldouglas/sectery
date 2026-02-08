package sectery.effects

trait Morbotron[F[_]]:
  def morbotron(q: String): F[List[String]]
