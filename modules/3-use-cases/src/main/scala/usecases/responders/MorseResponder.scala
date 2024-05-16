package sectery.usecases.responders

import sectery.control.Monad
import sectery.domain.entities._
import sectery.usecases.Responder

class MorseResponder[F[_]: Monad] extends Responder[F]:

  override def name = "@morse"

  override def usage = "@morse <encode|decode> <message>"

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

  override def respondToMessage(rx: Rx) =
    rx match
      case Rx(c, _, encode(message)) =>
        val encoded =
          message
            .toUpperCase()
            .split("")
            .flatMap(c => encoder.get(c))
            .mkString(" ")
        summon[Monad[F]].pure(List(Tx(c, encoded)))
      case Rx(c, _, decode(message)) =>
        val decoded =
          message
            .split(" ")
            .flatMap(c => decoder.get(c))
            .mkString("")
        summon[Monad[F]].pure(List(Tx(c, decoded)))
      case _ =>
        summon[Monad[F]].pure(Nil)
