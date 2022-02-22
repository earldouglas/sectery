package sectery

import zio.Task

class TestQueue[A](val q: zio.Queue[A]) extends Queue[A]:

  override def offer(as: Iterable[A]): Task[Boolean] =
    q.offerAll(as)

  override def take: Task[A] =
    q.take
