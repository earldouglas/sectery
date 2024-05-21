package sectery.effects

trait Frinkiac[F[_]]:
  def frinkiac(q: String): F[List[String]]
