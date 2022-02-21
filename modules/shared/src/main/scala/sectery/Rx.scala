package sectery

/** A message received from IRC.
  */
case class Rx(channel: String, nick: String, message: String)

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
