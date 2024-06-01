package sectery.domain.operations

import sectery.domain.entities.Rx

trait Receive[F[_]]:
  def name: String
  def apply(): F[Rx]
