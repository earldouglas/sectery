package sectery.producers

import sectery._
import zio._

object WeatherSpec extends ProducerSpec:

  override val http =
    ZLayer.succeed[Http.Service] {
      new Http.Service:
        def request(
            method: String,
            url: String,
            headers: Map[String, String],
            body: Option[String]
        ): ZIO[Any, Exception, Response] =
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
            case "https://api.openweathermap.org/data/3.0/onecall?lat=37.7790262&lon=-122.419906&exclude=minutely,hourly,daily,alerts&units=imperial&appid=alligator3" =>
              ZIO.succeed {
                Response(
                  status = 200,
                  headers = Map.empty,
                  body = Resource.read(
                    "/org/openweathermap/api/data/3.0/onecall?lat=37.7790262&lon=-122.419906&exclude=minutely,hourly,daily,alerts&units=imperial&appid=alligator3"
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
            case u =>
              ZIO.fail(new Exception(s"Unmocked URL: ${u}"))
    }

  override val specs =
    Map(
      "@wx produces weather" ->
        (
          List(Rx("#foo", "bar", "@wx san francisco")),
          List(
            Tx(
              "#foo",
              "San Francisco: 44°, humidity 65%, wind 10 mph, UV 0, O3 15 (Good), PM2.5 0 (Good)"
            )
          )
        ),
      "@wx produces weather in user-configured location" ->
        (
          List(
            Rx("#foo", "bar", "@wx"),
            Rx("#foo", "bar", "@set wx san francisco"),
            Rx("#foo", "bar", "@wx")
          ),
          List(
            Tx(
              "#foo",
              "bar: Set default location with `@set wx <location>`"
            ),
            Tx("#foo", "bar: wx set to san francisco"),
            Tx(
              "#foo",
              "San Francisco: 44°, humidity 65%, wind 10 mph, UV 0, O3 15 (Good), PM2.5 0 (Good)"
            )
          )
        )
    )
