package sectery.effects

trait Enqueue[F[_], A]:
  def name: String
  def apply(value: A): F[Unit]
