package sectery.usecases

import sectery.domain.entities.Tx

trait Announcer[F[_]]:
  def announce(): F[List[Tx]]
