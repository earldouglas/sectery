package sectery.effects

trait Dequeue[F[_], A]:
  def name: String
  def apply(): F[A]
