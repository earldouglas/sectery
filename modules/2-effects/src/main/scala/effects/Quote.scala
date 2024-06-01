package sectery.effects

import java.time.Instant

trait Quote[F[_]]:

  def quote(
      service: String,
      channel: String,
      nick: String
  ): F[Option[Quote.GrabbedMessage]]

  def quote(
      service: String,
      channel: String
  ): F[Option[Quote.GrabbedMessage]]

object Quote:
  case class GrabbedMessage(
      service: String,
      channel: String,
      nick: String,
      message: String,
      instant: Instant
  )
