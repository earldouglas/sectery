package sectery.producers

import sectery.Rx
import sectery.Tx

object AsciiSpec extends ProducerSpec:

  private val asciiFoo =
    List(
      "  ##",
      " #",
      " #",
      "####  ####    ####",
      " #   ##  ##  ##  ##",
      " #   #    #  #    #",
      " #   #    #  #    #",
      " #   #    #  #    #",
      " #   ##  ##  ##  ##",
      " #    ####    ####"
    )

  private val asciiFooTx =
    asciiFoo.map { line =>
      Tx("#foo", line)
    }

  private val asciiBlinkFooTx =
    asciiFoo.map { line =>
      Tx("#foo", s"\u0006${line}\u0006")
    }

  override val specs =
    Map(
      "@ascii produces ascii" ->
        (
          List(Rx("#foo", "bar", "@ascii foo")),
          asciiFooTx
        )
    )
