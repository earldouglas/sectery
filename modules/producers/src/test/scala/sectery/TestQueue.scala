package sectery

import zio.Task
import zio.stream.ZStream

class TestQueue[A](val q: zio.Queue[A]) extends Queue[A]:

  override def offer(as: Iterable[A]): Task[Boolean] =
    q.offerAll(as)

  override val take: ZStream[Any, Throwable, A] =
    ZStream.fromQueue(q)
