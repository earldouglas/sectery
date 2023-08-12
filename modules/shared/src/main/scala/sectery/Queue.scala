package sectery

import zio.Task
import zio.stream.ZStream

trait Queue[A]:

  def offer(as: Iterable[A]): Task[Unit]
  def take: ZStream[Any, Throwable, A]
