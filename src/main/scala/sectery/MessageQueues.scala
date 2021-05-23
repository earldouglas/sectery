package sectery

import java.io.IOException
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import zio.clock.Clock
import zio.duration._
import zio.Fiber
import zio.Queue
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
 * Read incoming messages, and optionally produce a response.
 */
object Producer {
  def apply(m: Rx): Iterable[Tx] =
    m match
      case Rx(c, _, "@ping") =>
        Some(Tx(c, "pong"))
      case _ =>
        None
}

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

  private def produce(inbox: Queue[Rx], outbox: Queue[Tx]): UIO[Unit] =
    for
      size    <- inbox.size
      message <- inbox.take
      txs      = Producer(message)
      _       <- ZIO.collectAll(txs.map(outbox.offer))
    yield ()

  private def send(outbox: Queue[Tx], sender: Sender): UIO[Unit] =
    for
      message <- outbox.take
      _       <- sender.send(message)
    yield ()

  def loop(sender: Sender): URIO[Clock, Queue[Rx]] =
    for
      inbox  <- ZQueue.unbounded[Rx]
      outbox <- ZQueue.unbounded[Tx]
      _      <- produce(inbox, outbox)
                  .forever
                  .fork
      _      <- send(outbox, sender)
                  .repeat(Schedule.spaced(250.milliseconds))
                  .fork
    yield inbox
