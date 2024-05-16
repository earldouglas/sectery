package sectery.effects

import sectery.domain.entities._

trait Autoquote[F[_]]:
  def getAutoquoteMessages(): F[List[Tx]]
