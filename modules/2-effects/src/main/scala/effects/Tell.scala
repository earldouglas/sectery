package sectery.effects

import java.time.Instant
import sectery.control.Monad._

trait Tell[F[_]]:
  def save(channel: String, m: Tell.Saved): F[Unit]
  def pop(channel: String, nick: String): F[List[Tell.Saved]]

object Tell:
  case class Saved(
      to: String,
      from: String,
      date: Instant,
      message: String
  )
