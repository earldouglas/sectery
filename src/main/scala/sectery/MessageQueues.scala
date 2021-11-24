package sectery

import sectery.Producer.Env
import zio.Fiber
import zio.Queue
import zio.RIO
import zio.Schedule
import zio.UIO
import zio.URIO
import zio.ZIO
import zio.ZQueue
import zio.durationInt

/** A message received from IRC.
  */
class Rx(
    val channel: String,
    val nick: String,
    val message: String
)

object Rx:
  def unapply(r: Rx): Option[(String, String, String)] =
    Some(
      (
        r.channel,
        r.nick,
        r.message
          .replaceAll("""^<[^>]*>""", "")
          .trim
      )
    )

/** A message to send to IRC.
  */
case class Tx(channel: String, message: String)

/** Read received messages from a queue, process them, queue them for
  * sending, and send them at a throttled rate.
  */
object MessageQueues:

  private def produce(
      inbox: Queue[Rx],
      outbox: Queue[Tx]
  ): URIO[Producer.Env, Unit] =
    for
      size <- inbox.size
      rx <- inbox.take
      txs <- produce_(
        channel = rx.channel,
        nick = rx.nick,
        message = rx.message
      )
      _ <- ZIO.collectAll(txs.map(outbox.offer))
    yield ()

  private def produce_(
      channel: String,
      nick: String,
      message: String
  ): URIO[Producer.Env, Iterable[Tx]] =
    message.split('|').toList match
      case Nil =>
        ZIO.succeed(Nil)
      case m :: Nil =>
        Producer(Rx(channel = channel, nick = nick, message = m))
      case m1 :: m2 :: mt =>
        for
          txs1 <- produce_(channel = channel, nick = nick, message = m1)
          txs2 <- ZIO
            .collectAll(
              txs1
                .map(_.message)
                .map(msg =>
                  val rest =
                    if mt.length > 0 then mt.mkString(" | ", " | ", "")
                    else ""
                  produce_(channel, nick, m2 + " " + msg + rest)
                )
            )
            .map(_.flatten)
        yield txs2

  val loop: RIO[Env, (Queue[Rx], Queue[Tx], Fiber[Nothing, Any])] =
    for
      _ <- Producer.init()
      inbox <- ZQueue.unbounded[Rx]
      outbox <- ZQueue.unbounded[Tx]
      _ <- Autoquote(outbox)
        .repeat(Schedule.spaced(5.minutes))
        .fork
      fiber <- produce(inbox, outbox).forever.fork
    yield (inbox, outbox, fiber)
