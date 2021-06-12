package sectery.producers

import sectery._
import zio.Inject._
import zio._
import zio.duration._
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
            case "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=90210" =>
              ZIO.effectTotal {
                Response(
                  status = 200,
                  headers = Map.empty,
                  body = """|[
                            |  {
                            |    "place_id": 260840887,
                            |    "licence": "Data © OpenStreetMap contributors, ODbL 1.0. https://osm.org/copyright",
                            |    "boundingbox": [
                            |      "33.935082673098",
                            |      "34.255082673098",
                            |      "-118.55932247265",
                            |      "-118.23932247265"
                            |    ],
                            |    "lat": "34.095082673097856",
                            |    "lon": "-118.39932247264568",
                            |    "display_name": "Beverly Hills, California, 90210, United States",
                            |    "class": "place",
                            |    "type": "postcode",
                            |    "importance": 0.33499999999999996
                            |  }
                            |]
                            |""".stripMargin
                )
              }
            case "https://api.darksky.net/forecast/alligator3/34.095082673097856,-118.39932247264568" =>
              ZIO.effectTotal {
                Response(
                  status = 200,
                  headers = Map.empty,
                  body = """|{
                            |  "latitude": 34.095082673097856,
                            |  "longitude": -118.39932247264568,
                            |  "timezone": "America/Los_Angeles",
                            |  "currently": {
                            |    "time": 1622806677,
                            |    "summary": "Clear",
                            |    "icon": "clear-night",
                            |    "nearestStormDistance": 11,
                            |    "nearestStormBearing": 42,
                            |    "precipIntensity": 0,
                            |    "precipProbability": 0,
                            |    "temperature": 55.99,
                            |    "apparentTemperature": 55.99,
                            |    "dewPoint": 55.11,
                            |    "humidity": 0.97,
                            |    "pressure": 1013.2,
                            |    "windSpeed": 1.87,
                            |    "windGust": 2.42,
                            |    "windBearing": 257,
                            |    "cloudCover": 0.2,
                            |    "uvIndex": 0,
                            |    "visibility": 10,
                            |    "ozone": 326.2
                            |  },
                            |  "minutely": {
                            |  },
                            |  "hourly": {
                            |  },
                            |  "daily": {
                            |  },
                            |  "alerts": [
                            |  ],
                            |  "flags": {
                            |  },
                            |  "offset": -7
                            |}
                            |""".stripMargin
                )
              }
            case "https://www.airnowapi.org/aq/observation/latLong/current/?format=application/json&latitude=34.095082673097856&longitude=-118.39932247264568&distance=50&API_KEY=alligator3" =>
              ZIO.effectTotal {
                Response(
                  status = 200,
                  headers = Map.empty,
                  body = """|[
                            |  {
                            |    "DateObserved": "2021-06-05 ",
                            |    "HourObserved": 10,
                            |    "LocalTimeZone": "PST",
                            |    "ReportingArea": "SW Coastal LA",
                            |    "StateCode": "CA",
                            |    "Latitude": 33.9541,
                            |    "Longitude": -118.4302,
                            |    "ParameterName": "O3",
                            |    "AQI": 22,
                            |    "Category": {
                            |      "Number": 1,
                            |      "Name": "Good"
                            |    }
                            |  },
                            |  {
                            |    "DateObserved": "2021-06-05 ",
                            |    "HourObserved": 10,
                            |    "LocalTimeZone": "PST",
                            |    "ReportingArea": "SW Coastal LA",
                            |    "StateCode": "CA",
                            |    "Latitude": 33.9541,
                            |    "Longitude": -118.4302,
                            |    "ParameterName": "PM2.5",
                            |    "AQI": 32,
                            |    "Category": {
                            |      "Number": 1,
                            |      "Name": "Good"
                            |    }
                            |  }
                            |]
                            |""".stripMargin
                )
              }
            case s"""https://www.airnowapi.org/aq/forecast/latLong/?format=application/json&latitude=34.095082673097856&longitude=-118.39932247264568&distance=50&API_KEY=alligator3""" =>
              ZIO.effectTotal {
                Response(
                  status = 200,
                  headers = Map.empty,
                  body = """|[
                            |  {
                            |    "DateIssue": "2021-06-11 ",
                            |    "DateForecast": "2021-06-12 ",
                            |    "ReportingArea": "SW Coastal LA",
                            |    "StateCode": "CA",
                            |    "Latitude": 33.9541,
                            |    "Longitude": -118.4302,
                            |    "ParameterName": "O3",
                            |    "AQI": 22,
                            |    "Category": {
                            |      "Number": 1,
                            |      "Name": "Good"
                            |    },
                            |    "ActionDay": false,
                            |    "Discussion": "..."
                            |  },
                            |  {
                            |    "DateIssue": "2021-06-11 ",
                            |    "DateForecast": "2021-06-12 ",
                            |    "ReportingArea": "SW Coastal LA",
                            |    "StateCode": "CA",
                            |    "Latitude": 33.9541,
                            |    "Longitude": -118.4302,
                            |    "ParameterName": "PM2.5",
                            |    "AQI": 32,
                            |    "Category": {
                            |      "Number": 1,
                            |      "Name": "Good"
                            |    },
                            |    "ActionDay": false,
                            |    "Discussion": "..."
                            |  },
                            |  {
                            |    "DateIssue": "2021-06-11 ",
                            |    "DateForecast": "2021-06-12 ",
                            |    "ReportingArea": "SW Coastal LA",
                            |    "StateCode": "CA",
                            |    "Latitude": 33.9541,
                            |    "Longitude": -118.4302,
                            |    "ParameterName": "PM10",
                            |    "AQI": 33,
                            |    "Category": {
                            |      "Number": 1,
                            |      "Name": "Good"
                            |    },
                            |    "ActionDay": false,
                            |    "Discussion": "..."
                            |  },
                            |  {
                            |    "DateIssue": "2021-06-11 ",
                            |    "DateForecast": "2021-06-13 ",
                            |    "ReportingArea": "SW Coastal LA",
                            |    "StateCode": "CA",
                            |    "Latitude": 33.9541,
                            |    "Longitude": -118.4302,
                            |    "ParameterName": "O3",
                            |    "AQI": 23,
                            |    "Category": {
                            |      "Number": 1,
                            |      "Name": "Good"
                            |    },
                            |    "ActionDay": false,
                            |    "Discussion": "..."
                            |  },
                            |  {
                            |    "DateIssue": "2021-06-11 ",
                            |    "DateForecast": "2021-06-13 ",
                            |    "ReportingArea": "SW Coastal LA",
                            |    "StateCode": "CA",
                            |    "Latitude": 33.9541,
                            |    "Longitude": -118.4302,
                            |    "ParameterName": "PM2.5",
                            |    "AQI": 33,
                            |    "Category": {
                            |      "Number": 1,
                            |      "Name": "Good"
                            |    },
                            |    "ActionDay": false,
                            |    "Discussion": "..."
                            |  },
                            |  {
                            |    "DateIssue": "2021-06-11 ",
                            |    "DateForecast": "2021-06-13 ",
                            |    "ReportingArea": "SW Coastal LA",
                            |    "StateCode": "CA",
                            |    "Latitude": 33.9541,
                            |    "Longitude": -118.4302,
                            |    "ParameterName": "PM10",
                            |    "AQI": 34,
                            |    "Category": {
                            |      "Number": 1,
                            |      "Name": "Good"
                            |    },
                            |    "ActionDay": false,
                            |    "Discussion": "..."
                            |  }
                            |]
                            |""".stripMargin
                )
              }
            case _ =>
              ZIO.effectTotal {
                Response(
                  status = 404,
                  headers = Map.empty,
                  body = ""
                )
              }
    }

  override def spec =
    suite(getClass().getName())(
      testM("@wx produces weather") {
        for
          sent <- ZQueue.unbounded[Tx]
          inbox <- MessageQueues
            .loop(new MessageLogger(sent))
            .inject(TestDb(), http)
          _ <- inbox.offer(Rx("#foo", "bar", "@wx 90210"))
          _ <- TestClock.adjust(1.seconds)
          ms <- sent.takeAll
        yield assert(ms)(
          equalTo(
            List(
              Tx(
                "#foo",
                "Beverly Hills, California, 90210, United States: temperature 56°, humidity 1.0%, wind 1.9 mph, UV index 0, O3: 22/Good, PM2.5: 32/Good, PM10: 33/Good"
              )
            )
          )
        )
      } @@ timeout(2.seconds)
    )
