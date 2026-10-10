package sectery.effects.id

import sectery.control.Monad
import sectery.control.Traversable

type Id[A] = A

given idMonad: Monad[Id] with
  override def pure[A](value: => A): Id[A] = value
  override def flatMap[A, B](fa: Id[A])(f: A => B): Id[B] = f(fa)

given idTraversable: Traversable[Id] with
  override def traverse[A, B](as: List[A])(
      f: A => Id[B]
  ): Id[List[B]] =
    as.map(f)
