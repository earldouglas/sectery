package sectery.effects

trait Grab[F[_]: Now]:
  def grab(service: String, channel: String, nick: String): F[Boolean]
