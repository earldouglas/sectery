package sectery.producers

import sectery._

object VersionSpec extends ProducerSpec:

  override def specs =
    Map(
      "@version produces version" ->
        (
          List(Rx("#foo", "bar", "@version")),
          List(Tx("#foo", "0.1.0-SNAPSHOT"))
        )
    )
