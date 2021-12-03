package sectery.producers

import sectery._

object ConfigSpec extends ProducerSpec:

  override val specs =
    Map(
      "@set and @get tz" ->
        (
          List(
            Rx("#foo", "bar", "@get tz"),
            Rx("#foo", "bar", "@set tz EST"),
            Rx("#foo", "bar", "@get tz"),
            Rx("#foo", "baz", "@get tz"),
            Rx("#foo", "baz", "@set tz PST"),
            Rx("#foo", "bar", "@get tz"),
            Rx("#foo", "baz", "@get tz"),
            Rx("#foo", "bar", "@set foo bar baz"),
            Rx("#foo", "bar", "@get foo")
          ),
          List(
            Tx("#foo", "bar: tz is not set"),
            Tx("#foo", "bar: tz set to EST"),
            Tx("#foo", "bar: tz is set to EST"),
            Tx("#foo", "baz: tz is not set"),
            Tx("#foo", "baz: tz set to PST"),
            Tx("#foo", "bar: tz is set to EST"),
            Tx("#foo", "baz: tz is set to PST"),
            Tx("#foo", "bar: foo set to bar baz"),
            Tx("#foo", "bar: foo is set to bar baz")
          )
        )
    )
