package sectery.effects

trait Stock[F[_]]:
  def getQuote(symbol: String): F[Option[String]]
