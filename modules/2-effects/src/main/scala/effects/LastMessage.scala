package sectery.effects

import java.time.Instant
import sectery.domain.entities.Rx

trait LastMessage[F[_]]:
  def saveLastMessage(rx: Rx): F[Unit]
  def getLastMessages(
      service: String,
      channel: String
  ): F[List[LastMessage.LastRx]]

object LastMessage:
  case class LastRx(
      service: String,
      channel: String,
      nick: String,
      message: String,
      instant: Instant
  )
