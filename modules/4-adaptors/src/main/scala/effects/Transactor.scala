package sectery.effects

import java.sql.Connection

trait Transactor[F[_]]:
  def transact[A](k: Connection => A): F[A]
