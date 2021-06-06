package sectery

import zio.Queue
import zio.UIO
import zio.ZIO

/** A [[Sender]] implementation for testing. Saves "sent" messages to a
  * queue for later inspection.
  */
class MessageLogger(queue: Queue[Tx]) extends Sender:
  def send(m: Tx): UIO[Unit] =
    queue.offer(m) *> ZIO.unit
