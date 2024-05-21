package sectery.effects

trait GetConfig[F[_]]:
  def getConfig(nick: String, key: String): F[Option[String]]
