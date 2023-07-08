package sectery.producers

import sectery._

object EvalSpec extends ProducerSpec:

  override def http =
    sys.env.get("TEST_HTTP_LIVE") match
      case Some("true") => Http.live
      case _            => TestHttp(200, Map.empty, "42")

  override val specs =
    Map(
      "@eval produces result" ->
        (
          List(Rx("#foo", "bar", "@eval 6 * 7")),
          List(Tx("#foo", "42"))
        )
    )
