package sectery.effects

trait Logger[F[_]]:
  def debug(message: => String): F[Unit]
  def error(message: => String): F[Unit]
