package sectery.producers

import sectery._

object SubstituteSpec extends ProducerSpec:

  override val specs =
    Map(
      "s/bar/baz/ replaces bar with baz" ->
        (
          List(
            Rx("#substitute", "foo", "I said bar first."),
            Rx("#substitute", "bar", "I said bar second."),
            Rx("#substitute", "baz", "I said bar third."),
            Rx("#substitute", "raz", "I didn't say it at all."),
            Rx("#substitute", "qux", "123 123 123"),
            Rx("#substitute", "waldo", "s/bar/baz/"),
            Rx("#substitute", "waldo", "s/1/one/"),
            Rx("#substitute", "waldo", "s/1/one/g")
          ),
          List(
            Tx("#substitute", "<baz> I said baz third."),
            Tx("#substitute", "<qux> one23 123 123"),
            Tx("#substitute", "<qux> one23 one23 one23")
          )
        )
    )
