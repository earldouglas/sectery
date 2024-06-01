package sectery.domain.entities

/** A message received.
  */
case class Rx(
    service: String,
    channel: String,
    thread: Option[String],
    nick: String,
    message: String
)
