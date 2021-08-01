package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test._
import zio.test.environment.TestClock

object WeatherSpec extends DefaultRunnableSpec:

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
            case "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=san+francisco" =>
              ZIO.succeed {
                Response(
                  status = 200,
                  headers = Map.empty,
                  body = Resource.read(
                    "/org/openstreetmap/nominatim/search?format=json&limit=1&q=san+francisco"
                  )
                )
              }
            case "https://api.darksky.net/forecast/alligator3/37.7790262,-122.419906" =>
              ZIO.succeed {
                Response(
                  status = 200,
                  headers = Map.empty,
                  body = Resource.read(
                    "/net/darksky/api/forecast/alligator3/37.7790262,-122.419906"
                  )
                )
              }
            case "https://www.airnowapi.org/aq/observation/latLong/current/?format=application/json&latitude=37.7790262&longitude=-122.419906&distance=50&API_KEY=alligator3" =>
              ZIO.succeed {
                Response(
                  status = 200,
                  headers = Map.empty,
                  body = Resource.read(
                    "/org/airnowapi/www/aq/observation/latLong/current/?format=application/json&latitude=37.7790262&longitude=-122.419906&distance=50&API_KEY=alligator3"
                  )
                )
              }
            case s"""https://www.airnowapi.org/aq/forecast/latLong/?format=application/json&latitude=37.7790262&longitude=-122.419906&distance=50&API_KEY=alligator3""" =>
              ZIO.succeed {
                Response(
                  status = 200,
                  headers = Map.empty,
                  body = Resource.read(
                    "/org/airnowapi/www/aq/forecast/latLong/?format=application/json&latitude=37.7790262&longitude=-122.419906&distance=50&API_KEY=alligator3"
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
      test("@wx produces weather") {
        for
          sent <- ZQueue.unbounded[Tx]
          (inbox, _) <- MessageQueues
            .loop(new MessageLogger(sent))
            .inject_(TestDb(), http)
          _ <- inbox.offer(Rx("#foo", "bar", "@wx san francisco"))
          _ <- TestClock.adjust(1.seconds)
          ms <- sent.takeAll
        yield assert(ms)(
          equalTo(
            List(
              Tx(
                "#foo",
                "San Francisco: 60° (low 53°, high 63°), humidity 96%, wind 6 mph, UV 0, O3 15 (Good), PM2.5 0 (Good)"
              )
            )
          )
        )
      } @@ timeout(2.seconds)
    )
