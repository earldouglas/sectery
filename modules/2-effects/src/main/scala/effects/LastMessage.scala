package sectery.effects

import java.time.Instant
import sectery.domain.entities.Rx

trait LastMessage[F[_]]:
  def saveLastMessage(rx: Rx): F[Unit]
  def getLastMessages(channel: String): F[List[LastMessage.LastRx]]

object LastMessage:
  case class LastRx(
      channel: String,
      nick: String,
      message: String,
      instant: Instant
  )
