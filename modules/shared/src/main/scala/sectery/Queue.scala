package sectery

import zio.Task

trait Queue[A]:

  def offer(as: Iterable[A]): Task[Boolean]
  def take: Task[A]
