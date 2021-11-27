package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test.TestClock
import zio.test._

object MorseSpec extends DefaultRunnableSpec:
  override def spec =
    suite(getClass().getName())(
      test("@morse encode produces morse") {
        for
          (inbox, outbox, _) <- MessageQueues.loop
            .inject_(TestDb(), TestHttp())
          _ <- inbox.offer(
            Rx(
              "#foo",
              "bar",
              "@morse encode The quick brown fox jumps over the lazy dog 1234567890"
            )
          )
          _ <- TestClock.adjust(1.seconds)
          m <- outbox.take
        yield assert(m)(
          equalTo(
            Tx(
              "#foo",
              "- .... . / --.- ..- .. -.-. -.- / -... .-. --- .-- -. / ..-. --- -..- / .--- ..- -- .--. ... / --- ...- . .-. / - .... . / .-.. .- --.. -.-- / -.. --- --. / .---- ..--- ...-- ....- ..... -.... --... ---.. ----. -----"
            )
          )
        )
      } @@ timeout(2.seconds),
      test("@morse decode produces text") {
        for
          (inbox, outbox, _) <- MessageQueues.loop
            .inject_(TestDb(), TestHttp())
          _ <- inbox.offer(
            Rx(
              "#foo",
              "bar",
              "@morse decode - .... . / --.- ..- .. -.-. -.- / -... .-. --- .-- -. / ..-. --- -..- / .--- ..- -- .--. ... / --- ...- . .-. / - .... . / .-.. .- --.. -.-- / -.. --- --. / .---- ..--- ...-- ....- ..... -.... --... ---.. ----. -----"
            )
          )
          _ <- TestClock.adjust(1.seconds)
          m <- outbox.take
        yield assert(m)(
          equalTo(
            Tx(
              "#foo",
              "THE QUICK BROWN FOX JUMPS OVER THE LAZY DOG 1234567890"
            )
          )
        )
      } @@ timeout(2.seconds)
    )
