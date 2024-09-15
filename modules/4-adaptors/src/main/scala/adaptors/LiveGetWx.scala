package sectery.adaptors

import java.net.URI
import java.net.URLEncoder
import sectery._
import sectery.control.Monad
import sectery.control.Monad._
import sectery.effects.HttpClient.Response
import sectery.effects._
import zio.json._

object LiveGetWx:
  def apply[F[_]: HttpClient: Monad](
      openWeatherMapApiKey: String,
      airNowApiKey: String
  ): GetWx[F] =
    new GetWx:

      object OSM:

        case class PlaceJson(
            display_name: String,
            lat: Double,
            lon: Double
        )

        object PlaceJson:
          implicit val decoder: JsonDecoder[PlaceJson] =
            DeriveJsonDecoder.gen[PlaceJson]

        case class Place(
            displayName: String,
            shortName: String,
            lat: Double,
            lon: Double
        )

        def findPlace(q: String): F[Option[Place]] =
          val qEnc = URLEncoder.encode(q, "UTF-8")
          summon[HttpClient[F]]
            .request(
              method = "GET",
              url = new URI(
                s"""https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${qEnc}"""
              ).toURL(),
              headers = Map("Accept" -> "application/json"),
              body = None
            )
            .map { case Response(200, _, body) =>
              body.fromJson[List[PlaceJson]] match
                case Right(pjs) =>
                  pjs.headOption.map(pj =>
                    Place(
                      displayName = pj.display_name,
                      shortName = pj.display_name
                        .split(", ")
                        .headOption
                        .getOrElse(q),
                      lat = pj.lat,
                      lon = pj.lon
                    )
                  )
                case Left(_) =>
                  None
            }

      object DarkSky:

        case class Currently(
            temperature: Float,
            humidity: Float,
            windSpeed: Float,
            windGust: Float,
            uvIndex: Int
        )

        object Currently:
          implicit val decoder: JsonDecoder[Currently] =
            DeriveJsonDecoder.gen[Currently]

        case class Data(
            temperatureHigh: Float,
            temperatureLow: Float
        )

        object Data:
          implicit val decoder: JsonDecoder[Data] =
            DeriveJsonDecoder.gen[Data]

        case class Daily(
            data: List[Data]
        )

        object Daily:
          implicit val decoder: JsonDecoder[Daily] =
            DeriveJsonDecoder.gen[Daily]

        case class ForecastJson(
            currently: Currently,
            daily: Daily
        )

        object ForecastJson:
          implicit val decoder: JsonDecoder[ForecastJson] =
            DeriveJsonDecoder.gen[ForecastJson]

        case class Forecast(
            temperature: Double,
            temperatureHigh: Double,
            temperatureLow: Double,
            humidity: Double,
            wind: Double,
            gusts: Double,
            uvIndex: Int
        )

        def findForecast(
            apiKey: String,
            lat: Double,
            lon: Double
        ): F[Option[Forecast]] =
          summon[HttpClient[F]]
            .request(
              method = "GET",
              url = new URI(
                s"""https://api.darksky.net/forecast/${apiKey}/${lat},${lon}"""
              ).toURL(),
              headers = Map(
                "User-Agent" -> "bot",
                "Accept" -> "application/json"
              ),
              body = None
            )
            .map { case Response(200, _, body) =>
              body.fromJson[ForecastJson] match
                case Right(fj) =>
                  fj.daily.data.headOption.map { dailyData =>
                    Forecast(
                      temperature = fj.currently.temperature,
                      temperatureHigh = dailyData.temperatureHigh,
                      temperatureLow = dailyData.temperatureLow,
                      humidity = fj.currently.humidity * 100,
                      wind = fj.currently.windSpeed,
                      gusts = fj.currently.windGust,
                      uvIndex = fj.currently.uvIndex
                    )
                  }
                case Left(_) =>
                  None
            }

      object OpenWeatherMap:

        case class OneCall(
            current: Current
        )

        object OneCall:
          implicit val decoder: JsonDecoder[OneCall] =
            DeriveJsonDecoder.gen[OneCall]

        case class Weather(
            main: String,
            description: String
        )

        object Weather:
          implicit val decoder: JsonDecoder[Weather] =
            DeriveJsonDecoder.gen[Weather]

        case class Current(
            temp: Float,
            humidity: Float,
            wind_speed: Float,
            uvi: Float,
            weather: List[Weather]
        )

        object Current:
          implicit val decoder: JsonDecoder[Current] =
            DeriveJsonDecoder.gen[Current]

        def findCurrent(
            apiKey: String,
            lat: Double,
            lon: Double
        ): F[Option[Current]] =
          summon[HttpClient[F]]
            .request(
              method = "GET",
              url = new URI(
                s"""https://api.openweathermap.org/data/3.0/onecall?lat=${lat}&lon=${lon}&exclude=minutely,hourly,daily,alerts&units=imperial&appid=${apiKey}"""
              ).toURL(),
              headers = Map(
                "User-Agent" -> "bot",
                "Accept" -> "application/json"
              ),
              body = None
            )
            .map { case Response(200, _, body) =>
              body.fromJson[OneCall] match
                case Right(x) => Some(x.current)
                case Left(e)  => None
            }

      object AirNowObservation:

        case class Category(
            Name: String
        )

        object Category:
          implicit val decoder: JsonDecoder[Category] =
            DeriveJsonDecoder.gen[Category]

        case class Observation(
            ParameterName: String,
            AQI: Int,
            Category: Category
        )

        object Observation:
          implicit val decoder: JsonDecoder[Observation] =
            DeriveJsonDecoder.gen[Observation]

        case class AqiParameter(
            name: String,
            value: Int,
            category: String
        )
        case class Aqi(parameters: List[AqiParameter])

        def findAqi(
            apiKey: String,
            lat: Double,
            lon: Double
        ): F[Option[Aqi]] =
          summon[HttpClient[F]]
            .request(
              method = "GET",
              url = new URI(
                s"""https://www.airnowapi.org/aq/observation/latLong/current/?format=application/json&latitude=${lat}&longitude=${lon}&distance=50&API_KEY=${apiKey}"""
              ).toURL(),
              headers = Map(
                "User-Agent" -> "bot",
                "Accept" -> "application/json"
              ),
              body = None
            )
            .map { case Response(200, _, body) =>
              body.fromJson[List[Observation]] match
                case Right(os) =>
                  Some(
                    Aqi(
                      parameters = os.map { o =>
                        AqiParameter(
                          name = o.ParameterName,
                          value = o.AQI,
                          category = o.Category.Name
                        )
                      }
                    )
                  )
                case Left(_) =>
                  None
            }

      object AirNowForecast:

        case class Category(
            Name: String
        )

        object Category:
          implicit val decoder: JsonDecoder[Category] =
            DeriveJsonDecoder.gen[Category]

        case class Forecast(
            DateForecast: String,
            ParameterName: String,
            AQI: Int,
            Category: Category
        )

        object Forecast:
          implicit val decoder: JsonDecoder[Forecast] =
            DeriveJsonDecoder.gen[Forecast]

        case class AqiParameter(
            name: String,
            date: String,
            value: Int,
            category: String
        )
        case class Aqi(parameters: List[AqiParameter])

        def findAqi(
            apiKey: String,
            lat: Double,
            lon: Double
        ): F[Option[Aqi]] =
          summon[HttpClient[F]]
            .request(
              method = "GET",
              url = new URI(
                s"""https://www.airnowapi.org/aq/forecast/latLong/?format=application/json&latitude=${lat}&longitude=${lon}&distance=50&API_KEY=${apiKey}"""
              ).toURL(),
              headers = Map(
                "User-Agent" -> "bot",
                "Accept" -> "application/json"
              ),
              body = None
            )
            .map { case Response(200, _, body) =>
              body.fromJson[List[Forecast]] match
                case Right(fs) if fs.length > 0 =>
                  Some(
                    Aqi(
                      parameters = fs.map { f =>
                        AqiParameter(
                          name = f.ParameterName,
                          date = f.DateForecast,
                          value = f.AQI,
                          category = f.Category.Name
                        )
                      }
                    )
                  )
                case _ =>
                  None
            }

      case class AqiParameter(
          name: String,
          value: Int,
          category: String
      )

      private def findWx(
          lat: Double,
          lon: Double
      ): F[Option[String]] =
        for
          currentO <- OpenWeatherMap.findCurrent(
            apiKey = openWeatherMapApiKey,
            lat = lat,
            lon = lon
          )
          aqiObservationO <- AirNowObservation.findAqi(
            apiKey = airNowApiKey,
            lat = lat,
            lon = lon
          )
          aqiForecastO <- AirNowForecast.findAqi(
            apiKey = airNowApiKey,
            lat = lat,
            lon = lon
          )
        yield for
          current <- currentO
          aqiObservation <- aqiObservationO
          aqiForecast <- aqiForecastO
        yield
          var aqiMap: Map[String, AqiParameter] = Map.empty
          aqiObservation.parameters.foreach { case p =>
            if !aqiMap.contains(p.name) then
              aqiMap = aqiMap + (p.name -> AqiParameter(
                name = p.name,
                value = p.value,
                category = p.category
              ))
          }
          aqiForecast.parameters.sortBy(_.date).foreach { case p =>
            if !aqiMap.contains(p.name) then
              aqiMap = aqiMap + (p.name -> AqiParameter(
                name = p.name,
                value = p.value,
                category = p.category
              ))
          }
          (
            List(
              f"${current.temp}%.0f°",
              f"hum ${current.humidity}%.0f%%",
              f"wnd ${current.wind_speed}%.0f mph"
            ) ++ current.weather.map { w =>
              w.description
            } ++ List(
              f"uv ${current.uvi.toInt}"
            ) ++ aqiMap.values.map { p =>
              s"${p.name.toLowerCase} ${p.value}"
            }
          ).mkString(", ")

      private def findWx(
          q: String
      ): F[String] =
        OSM.findPlace(q).flatMap {
          case Some(p @ OSM.Place(_, _, lat, lon)) =>
            findWx(lat, lon) flatMap {
              case Some(wx) =>
                summon[Monad[F]].pure(f"${p.shortName}: ${wx}")
              case None =>
                summon[Monad[F]]
                  .pure(s"I can't find the weather for ${q}.")
            }
          case None =>
            summon[Monad[F]].pure(s"I have no idea where ${q} is.")
        }

      override def getWx(location: String): F[String] =
        findWx(location)
