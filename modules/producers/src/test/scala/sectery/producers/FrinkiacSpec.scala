package sectery.producers

import sectery._
import zio._

object FrinkiacSpec extends ProducerSpec:

  override val http: ULayer[Http.Service] =
    ZLayer.succeed[Http.Service] {
      new Http.Service:
        def request(
            method: String,
            url: String,
            headers: Map[String, String],
            body: Option[String]
        ): ZIO[Any, Nothing, Response] =
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

  override val specs =
    Map(
      "@frinkiac produces quote" ->
        (
          List(
            Rx("#foo", "bar", "@frinkiac i got to think of a lie fast")
          ),
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
