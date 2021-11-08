package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test._
import zio.test.environment.TestClock

object FrinkiacSpec extends DefaultRunnableSpec:

  val http: ULayer[Has[Http.Service]] =
    ZLayer.succeed {
      new Http.Service:
        def request(
            method: String,
            url: String,
            headers: Map[String, String],
            body: Option[String]
        ): UIO[Response] =
          url match
            case "https://frinkiac.com/api/search?q=i+got+to+think+of+a+lie+fast" =>
              ZIO.succeed {
                Response(
                  status = 200,
                  headers = Map.empty,
                  body = Resource.read(
                    "/com/frinkiac/api/search?q=i+got+to+think+of+a+lie+fast"
                  )
                )
              }
            case "https://frinkiac.com/api/caption?e=S04E16&t=166465" =>
              ZIO.succeed {
                Response(
                  status = 200,
                  headers = Map.empty,
                  body = Resource.read(
                    "/com/frinkiac/api/caption?e=S04E16&t=166465"
                  )
                )
              }
            case _ =>
              ZIO.succeed {
                Response(
                  status = 404,
                  headers = Map.empty,
                  body = ""
                )
              }
    }

  override def spec =
    suite(getClass().getName())(
      test("@frinkiac produces url") {
        for
          (inbox, outbox, _) <- MessageQueues.loop
            .inject_(TestDb(), http)
          _ <- inbox.offer(
            Rx("#foo", "bar", "@frinkiac i got to think of a lie fast")
          )
          _ <- TestClock.adjust(1.seconds)
          ms <- outbox.takeAll
        yield assert(ms)(
          equalTo(
            List(
              Tx(
                "#foo",
                "https://frinkiac.com/caption/S04E16/166465"
              ),
              Tx(
                "#foo",
                "DID I SAY THAT OR JUST THINK IT?"
              ),
              Tx(
                "#foo",
                "I GOT TO THINK OF A LIE FAST."
              ),
              Tx(
                "#foo",
                "HOMER, ARE YOU GOING TO THE DUFF BREWERY?"
              )
            )
          )
        )
      } @@ timeout(2.seconds)
    )
