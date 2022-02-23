package sectery

import zio.Task
import zio.stream.ZStream

trait Queue[A]:

  def offer(as: Iterable[A]): Task[Boolean]
  def take: ZStream[Any, Throwable, A]
