package sectery.effects

trait Btc[F[_]]:
  def toUsd(): F[Option[Float]]
