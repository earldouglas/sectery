package sectery.effects

trait Points[F[_]]:
  def update(channel: String, nick: String, delta: Int): F[Long]
