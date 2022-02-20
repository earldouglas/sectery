package sectery.producers

import sectery._

object HelpSpec extends ProducerSpec:

  override val specs =
    Map(
      "@help produces help" ->
        (
          List(
            Rx("#foo", "bar", "@help"),
            Rx("#foo", "bar", "@help @wx"),
            Rx("#foo", "bar", "@help @count"),
            Rx("#foo", "bar", "@help @foo")
          ),
          List(
            Tx(
              "#foo",
              List(
                "@ascii",
                "@blink",
                "@btc",
                "@count",
                "@eval",
                "@frinkiac",
                "@get",
                "@grab",
                "@hack",
                "@morse",
                "@ping",
                "@quote",
                "@set",
                "@stock",
                "@tell",
                "@time",
                "@version",
                "@wx",
                "s///"
              ).mkString(", ")
            ),
            Tx(
              "#foo",
              "Usage: @wx [location], e.g. @wx san francisco"
            ),
            Tx("#foo", "Usage: @count"),
            Tx("#foo", "I don't know anything about @foo")
          )
        )
    )
