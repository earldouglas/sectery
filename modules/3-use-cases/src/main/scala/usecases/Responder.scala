package sectery.usecases

import sectery.domain.entities.Rx
import sectery.domain.entities.Tx

trait Responder[F[_]]:
  def name: String
  def usage: String
  def respondToMessage(rx: Rx): F[List[Tx]]
