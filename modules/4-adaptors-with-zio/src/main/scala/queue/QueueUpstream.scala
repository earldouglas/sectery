package sectery.queue

import sectery.domain.entities._
import sectery.effects._
import sectery.usecases._
import zio.Fiber
import zio.ZIO
import zio.stream.ZStream

object QueueUpstream:

  type LoggerR = Logger[[A] =>> ZIO[Any, Throwable, A]]
  type DequeueR = Dequeue[[A] =>> ZStream[Any, Throwable, A], Rx]
  type EnqueueR = Enqueue[[A] =>> ZIO[Any, Throwable, A], Tx]
  type RespondersR = Responders[[A] =>> ZIO[Any, Throwable, A]]

  type Env = LoggerR & DequeueR & EnqueueR & RespondersR

  def respondLoop(): ZIO[Env, Nothing, Fiber[Throwable, Unit]] =
    for
      logger <- ZIO.environmentWith[LoggerR](_.get)
      dequeue <- ZIO.environmentWith[DequeueR](_.get)
      enqueue <- ZIO.environmentWith[EnqueueR](_.get)
      responders <- ZIO.environmentWith[RespondersR](_.get)
      dequeueAndSendFiber <-
        dequeue()
          .tap(rx =>
            logger.debug(s"dequeued ${rx} from ${dequeue.name}")
          )
          .mapZIO(rx =>
            responders
              .respondToMessage(rx)
              .catchAllCause(cause =>
                ZIO
                  .logError(cause.prettyPrint)
                  .zipRight(ZIO.succeed(List.empty[Tx]))
              )
              .tap(txs =>
                ZIO.collectAll(
                  txs.map(tx =>
                    logger.debug(s"enqueuing ${tx} in ${enqueue.name}")
                  )
                )
              )
              .flatMap(txs =>
                ZIO.collectAll(txs.map(tx => enqueue(tx)))
              )
          )
          .runDrain
          .fork
    yield dequeueAndSendFiber
