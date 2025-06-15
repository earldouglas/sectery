package sectery.effects

trait Grab[F[_]]:
  def grab(service: String, channel: String, nick: String): F[Boolean]
