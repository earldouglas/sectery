package sectery

import zio.clock.Clock
import zio.duration._
import zio.Has
import zio.Queue
import zio.RIO
import zio.Schedule
import zio.UIO
import zio.URIO
import zio.ZIO
import zio.ZQueue

/**
 * A message received from IRC.
 */
case class Rx(channel: String, nick: String, message: String)

/**
 * A message to send to IRC.
 */
case class Tx(channel: String, message: String)

/**
 * An interface for defining how to send an outgoing message, which
 * varies between testing and production.
 */
trait Sender:
  def send(m: Tx): UIO[Unit]

/**
 * Read received messages from a queue, process them, queue them for
 * sending, and send them at a throttled rate.
 */
object MessageQueues:

  private def produce(inbox: Queue[Rx], outbox: Queue[Tx]): URIO[Producer.Env, Unit] =
    for
      size    <- inbox.size
      message <- inbox.take
      txs     <- Producer(message)
      _       <- ZIO.collectAll(txs.map(outbox.offer))
    yield ()

  private def send(outbox: Queue[Tx], sender: Sender): UIO[Unit] =
    for
      message <- outbox.take
      _       <- sender.send(message)
    yield ()

  def loop(sender: Sender): RIO[Producer.Env, Queue[Rx]] =
    for
      _      <- Producer.init()
      inbox  <- ZQueue.unbounded[Rx]
      outbox <- ZQueue.unbounded[Tx]
      _      <- produce(inbox, outbox)
                  .forever
                  .fork
      _      <- send(outbox, sender)
                  .repeat(Schedule.spaced(250.milliseconds))
                  .fork
    yield inbox
