package sectery.producers

import sectery.Producer
import sectery.Rx
import sectery.Tx
import zio.ZIO

class Help(producers: List[Producer]) extends Producer:

  private val usage = """^@help\s+(.+)\s*$""".r

  private val helpMessage: String =
    s"""${producers
        .flatMap(p => p.help().map(_.name))
        .sorted
        .mkString(", ")}"""

  private val usageMap: Map[String, String] =
    producers.flatMap(_.help().map(i => i.name -> i.usage)).toMap

  override def help(): Iterable[Info] =
    None

  override def apply(m: Rx): ZIO[Any, Nothing, Iterable[Tx]] =
    ZIO.succeed {
      m match
        case Rx(channel, _, "@help") =>
          Some(Tx(m.channel, helpMessage))
        case Rx(channel, _, usage(name)) =>
          usageMap.get(name) match
            case Some(usage) =>
              Some(Tx(channel, s"Usage: ${usage}"))
            case None =>
              Some(Tx(channel, s"I don't know anything about ${name}"))
        case _ =>
          None
    }

object Help {
  def apply(producers: List[Producer]): List[Producer] =
    new Help(producers) :: producers
}
