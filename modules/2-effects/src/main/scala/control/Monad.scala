package sectery.control

trait Monad[F[_]]:
  def pure[A](value: => A): F[A]
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
  def map[A, B](fa: F[A])(f: A => B): F[B] =
    flatMap(fa)(a => pure(f(a)))

object Monad:
  extension [A, F[_]: Monad](fa: F[A])
    def flatMap[B](f: A => F[B]): F[B] =
      summon[Monad[F]].flatMap(fa)(f)
    def map[B](f: A => B): F[B] =
      summon[Monad[F]].map(fa)(f)
