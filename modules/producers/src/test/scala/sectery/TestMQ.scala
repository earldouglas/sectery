package sectery

import zio.ZIO
import zio.ZLayer

object TestMQ:
  def apply(): ZIO[Any, Nothing, ZLayer[Any, Nothing, MessageQueue]] =
    for
      zInbox <- zio.Queue.unbounded[Rx]
      zOutbox <- zio.Queue.unbounded[Tx]
    yield ZLayer.succeed:
      new MessageQueue:
        override def inbox = new TestQueue(zInbox)
        override def outbox = new TestQueue(zOutbox)
