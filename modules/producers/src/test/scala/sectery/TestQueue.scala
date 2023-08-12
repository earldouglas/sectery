package sectery

import zio.Task
import zio.ZIO
import zio.stream.ZStream

class TestQueue[A](val q: zio.Queue[A]) extends Queue[A]:

  override def offer(as: Iterable[A]): Task[Unit] =
    q.offerAll(as) *> ZIO.succeed(())

  override val take: ZStream[Any, Throwable, A] =
    ZStream.fromQueue(q)
