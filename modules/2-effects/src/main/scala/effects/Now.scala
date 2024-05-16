package sectery.effects

import java.time.Instant

trait Now[F[_]]:
  def now(): F[Instant]
