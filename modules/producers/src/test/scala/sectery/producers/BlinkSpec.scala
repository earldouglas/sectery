package sectery.producers

import sectery.Rx
import sectery.Tx

object BlinkSpec extends ProducerSpec:

  override val specs =
    Map(
      "@blink foo produces blinking foo" ->
        (
          List(Rx("#foo", "bar", "@blink foo")),
          List(Tx("#foo", "\u0006foo\u0006"))
        )
    )
