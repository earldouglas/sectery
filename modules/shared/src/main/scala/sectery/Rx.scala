package sectery

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
