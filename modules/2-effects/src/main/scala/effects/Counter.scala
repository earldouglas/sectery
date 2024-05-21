package sectery.effects

trait Counter[F[_]]:
  def incrementAndGet(): F[Int]
