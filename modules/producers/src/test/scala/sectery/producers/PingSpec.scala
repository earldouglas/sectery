package sectery.producers

import sectery._

object PingSpec extends ProducerSpec:

  override val specs =
    Map(
      "@ping produces pong" ->
        (
          List(Rx("#foo", "bar", "@ping")),
          List(Tx("#foo", "pong"))
        )
    )
