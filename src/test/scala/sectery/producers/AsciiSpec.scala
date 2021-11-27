package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test.TestClock
import zio.test._

object AsciiSpec extends DefaultRunnableSpec:

  private val asciiFoo =
    List(
      "    ##",
      "   #",
      "   #",
      " #####   ###    ###",
      "   #    #   #  #   #",
      "   #    #   #  #   #",
      "   #    #   #  #   #",
      "   #    #   #  #   #",
      "   #    #   #  #   #",
      "   #     ###    ###"
    )

  private val asciiFooTx =
    asciiFoo.map { line =>
      Tx("#foo", line)

    }

  private val asciiBlinkFooTx =
    asciiFoo.map { line =>
      Tx("#foo", s"\u0006${line}\u0006")
    }

  override def spec =
    suite(getClass().getName())(
      test("@ascii produces ascii") {
        for
          (inbox, outbox, _) <- MessageQueues.loop
            .inject_(TestDb(), TestHttp())
          _ <- inbox.offer(Rx("#foo", "bar", "@ascii foo"))
          _ <- inbox.offer(Rx("#foo", "bar", "@ascii foo | @blink"))
          _ <- TestClock.adjust(1.seconds)
          ms <- outbox.takeAll
        yield assert(ms)(equalTo(asciiFooTx ++ asciiBlinkFooTx))
      } @@ timeout(2.seconds)
    )
