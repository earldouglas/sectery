package sectery

import zio.ZIO

trait MessageQueue:
  def inbox: Queue[Rx]
  def outbox: Queue[Tx]

object MessageQueue:

  def inbox: ZIO[MessageQueue, Nothing, Queue[Rx]] =
    ZIO.environmentWithZIO(r => ZIO.succeed(r.get.inbox))

  def outbox: ZIO[MessageQueue, Nothing, Queue[Tx]] =
    ZIO.environmentWithZIO(r => ZIO.succeed(r.get.outbox))
