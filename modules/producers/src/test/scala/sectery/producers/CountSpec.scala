package sectery.producers

import sectery._

object CountSpec extends ProducerSpec:

  override val specs =
    Map(
      "@count produces count" ->
        (
          List(
            Rx("#foo", "bar", "@count"),
            Rx("#foo", "bar", "@count"),
            Rx("#foo", "bar", "@count")
          ),
          List(
            Tx("#foo", "1"),
            Tx("#foo", "2"),
            Tx("#foo", "3")
          )
        )
    )
