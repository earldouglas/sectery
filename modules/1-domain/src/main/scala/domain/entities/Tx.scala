package sectery.domain.entities

/** A message to send.
  */
case class Tx(
    service: String,
    channel: String,
    thread: Option[String],
    message: String
)
