package sectery.control

trait Traversable[F[_]]:
  def traverse[A, B](as: List[A])(f: A => F[B]): F[List[B]]
