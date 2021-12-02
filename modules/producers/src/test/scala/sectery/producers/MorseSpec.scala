package sectery.producers

import sectery._

object MorseSpec extends ProducerSpec:

  override val specs =
    Map(
      "@morse encode produces morse" ->
        (
          List(
            Rx(
              "#foo",
              "bar",
              "@morse encode The quick brown fox jumps over the lazy dog 1234567890"
            )
          ),
          List(
            Tx(
              "#foo",
              "- .... . / --.- ..- .. -.-. -.- / -... .-. --- .-- -. / ..-. --- -..- / .--- ..- -- .--. ... / --- ...- . .-. / - .... . / .-.. .- --.. -.-- / -.. --- --. / .---- ..--- ...-- ....- ..... -.... --... ---.. ----. -----"
            )
          )
        ),
      "@morse decode produces text" ->
        (
          List(
            Rx(
              "#foo",
              "bar",
              "@morse decode - .... . / --.- ..- .. -.-. -.- / -... .-. --- .-- -. / ..-. --- -..- / .--- ..- -- .--. ... / --- ...- . .-. / - .... . / .-.. .- --.. -.-- / -.. --- --. / .---- ..--- ...-- ....- ..... -.... --... ---.. ----. -----"
            )
          ),
          List(
            Tx(
              "#foo",
              "THE QUICK BROWN FOX JUMPS OVER THE LAZY DOG 1234567890"
            )
          )
        )
    )
