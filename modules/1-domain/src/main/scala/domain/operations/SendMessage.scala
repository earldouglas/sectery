package sectery.domain.operations

import sectery.domain.entities.Tx

trait SendMessage[F[_]]:
  def sendMessage(tx: Tx): F[Unit]
