package sectery.effects

trait Points[F[_]]:
  def update(
      service: String,
      channel: String,
      nick: String,
      delta: Int
  ): F[Long]
