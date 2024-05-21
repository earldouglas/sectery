package sectery.effects

trait GetWx[F[_]]:
  def getWx(location: String): F[String]
