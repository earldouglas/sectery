package sectery.domain.operations

import sectery.domain.entities.Rx

trait ReceiveMessage[F[_]]:
  def receiveMessage(rx: Rx): F[Unit]
