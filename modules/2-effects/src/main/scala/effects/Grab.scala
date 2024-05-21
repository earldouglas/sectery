package sectery.effects

trait Grab[F[_]: Now]:
  def grab(channel: String, nick: String): F[Boolean]
  def grab(channel: String): F[Option[String]]
