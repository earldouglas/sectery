package sectery.queue

import sectery.domain.entities._
import sectery.domain.operations._
import sectery.effects._
import zio.Fiber
import zio.ZIO
import zio.stream.ZStream

object QueueDownstream:

  type LoggerR = Logger[[A] =>> ZIO[Any, Throwable, A]]
  type ReceiveR = Receive[[A] =>> ZIO[Any, Throwable, A]]
  type EnqueueR = Enqueue[[A] =>> ZIO[Any, Throwable, A], Rx]
  type SendR = Send[[A] =>> ZIO[Any, Throwable, A]]
  type DequeueR = Dequeue[[A] =>> ZStream[Any, Throwable, A], Tx]

  type Env = LoggerR & ReceiveR & EnqueueR & DequeueR & SendR

  def respondLoop(): ZIO[Env, Nothing, Fiber[Throwable, Unit]] =
    for
      receiveAndEnqueueFiber <- receiveAndEnqueue()
      dequeueAndSendFiber <- dequeueAndSend()
      fiber <- Fiber
        .joinAll(List(receiveAndEnqueueFiber, dequeueAndSendFiber))
        .fork
    yield fiber

  private def receiveAndEnqueue() =
    for
      logger <- ZIO.environmentWith[LoggerR](_.get)
      enqueue <- ZIO.environmentWith[EnqueueR](_.get)
      receive <- ZIO.environmentWith[ReceiveR](_.get)
      receiveAndEnqueueFiber <-
        receive()
          .tap(rx =>
            logger.debug(s"received ${rx} from ${receive.name}")
          )
          .tap(rx =>
            logger.debug(s"enqueuing ${rx} in ${enqueue.name}")
          )
          .flatMap(rx => enqueue(rx))
          .forever
          .fork
    yield receiveAndEnqueueFiber

  private def dequeueAndSend() =
    for
      logger <- ZIO.environmentWith[LoggerR](_.get)
      dequeue <- ZIO.environmentWith[DequeueR](_.get)
      send <- ZIO.environmentWith[SendR](_.get)
      dequeueAndSendFiber <-
        dequeue()
          .tap(tx =>
            logger.debug(s"dequeued ${tx} from ${dequeue.name}")
          )
          .tap(tx => logger.debug(s"sending ${tx} to ${send.name}"))
          .mapZIO(tx => send(tx))
          .runDrain
          .fork
    yield dequeueAndSendFiber
