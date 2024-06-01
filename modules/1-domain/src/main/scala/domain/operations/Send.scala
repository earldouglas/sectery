package sectery.domain.operations

import sectery.domain.entities.Tx

trait Send[F[_]]:
  def name: String
  def apply(tx: Tx): F[Unit]
