package sectery.effects

trait SetConfig[F[_]]:
  def setConfig(nick: String, key: String, value: String): F[Unit]
