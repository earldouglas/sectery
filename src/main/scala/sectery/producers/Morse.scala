package sectery.producers

import sectery.Info
import sectery.Producer
import sectery.Rx
import sectery.Tx
import zio.Clock
import zio.URIO
import zio.ZIO

object Morse extends Producer:

  private val encoder: Map[String, String] =
    Map(
      "A" -> ".-",
      "B" -> "-...",
      "C" -> "-.-.",
      "D" -> "-..",
      "E" -> ".",
      "F" -> "..-.",
      "G" -> "--.",
      "H" -> "....",
      "I" -> "..",
      "J" -> ".---",
      "K" -> "-.-",
      "L" -> ".-..",
      "M" -> "--",
      "N" -> "-.",
      "O" -> "---",
      "P" -> ".--.",
      "Q" -> "--.-",
      "R" -> ".-.",
      "S" -> "...",
      "T" -> "-",
      "U" -> "..-",
      "V" -> "...-",
      "W" -> ".--",
      "X" -> "-..-",
      "Y" -> "-.--",
      "Z" -> "--..",
      "1" -> ".----",
      "2" -> "..---",
      "3" -> "...--",
      "4" -> "....-",
      "5" -> ".....",
      "6" -> "-....",
      "7" -> "--...",
      "8" -> "---..",
      "9" -> "----.",
      "0" -> "-----",
      " " -> "/"
    )

  private val decoder: Map[String, String] =
    encoder.map { (e, m) => (m, e) }

  private val encode = """^@morse\s+encode\s+(.+)$""".r
  private val decode = """^@morse\s+decode\s+(.+)$""".r

  override def help(): Iterable[Info] =
    Some(Info("@morse", "@morse <encode|decode> <message>"))

  override def apply(m: Rx): URIO[Clock, Iterable[Tx]] =
    m match
      case Rx(c, _, encode(message)) =>
        val encoded =
          message
            .toUpperCase()
            .split("")
            .flatMap(c => encoder.get(c))
            .mkString(" ")
        ZIO.succeed(Some(Tx(c, encoded)))
      case Rx(c, _, decode(message)) =>
        val decoded =
          message
            .split(" ")
            .flatMap(c => decoder.get(c))
            .mkString("")
        ZIO.succeed(Some(Tx(c, decoded)))
      case _ =>
        ZIO.succeed(None)
