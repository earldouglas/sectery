package sectery.effects

import java.time.Instant

trait Quote[F[_]]:
  def quote(
      channel: String,
      nick: String
  ): F[Option[Quote.GrabbedMessage]]
  def quote(channel: String): F[Option[Quote.GrabbedMessage]]

object Quote:
  case class GrabbedMessage(
      channel: String,
      nick: String,
      message: String,
      instant: Instant
  )
