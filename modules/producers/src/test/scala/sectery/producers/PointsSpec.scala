package sectery.producers

import sectery._

object PointsSpec extends ProducerSpec:

  override val specs =
    Map(
      "plus one" ->
        (
          List(
            Rx("#foo", "alice", "bob++"),
            Rx("#foo", "alice", "alice++"),
            Rx("#foo", "alice", "bob++")
          ),
          List(
            Tx("#foo", "bob has 1 point."),
            Tx("#foo", "You gotta get someone else to do it."),
            Tx("#foo", "bob has 2 points.")
          )
        ),
      "minus one" ->
        (
          List(
            Rx("#foo", "alice", "bob--"),
            Rx("#foo", "alice", "bob--"),
            Rx("#foo", "alice", "bob--")
          ),
          List(
            Tx("#foo", "bob has -1 points."),
            Tx("#foo", "bob has -2 points."),
            Tx("#foo", "bob has -3 points.")
          )
        ),
      "plus/minus one" ->
        (
          List(
            Rx("#foo", "alice", "bob++"),
            Rx("#foo", "alice", "alice++"),
            Rx("#foo", "alice", "bob++"),
            Rx("#foo", "alice", "bob--"),
            Rx("#foo", "alice", "bob--"),
            Rx("#foo", "alice", "bob--")
          ),
          List(
            Tx("#foo", "bob has 1 point."),
            Tx("#foo", "You gotta get someone else to do it."),
            Tx("#foo", "bob has 2 points."),
            Tx("#foo", "bob has 1 point."),
            Tx("#foo", "bob has 0 points."),
            Tx("#foo", "bob has -1 points.")
          )
        )
    )
