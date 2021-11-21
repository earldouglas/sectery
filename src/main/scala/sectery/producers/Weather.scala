package sectery.producers

import java.net.URLEncoder
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import scala.collection.JavaConverters._
import sectery.Http
import sectery.Info
import sectery.Producer
import sectery.Response
import sectery.Rx
import sectery.Tx
import zio.Clock
import zio.RIO
import zio.ZIO
import zio.json._

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

  def findPlace(q: String): RIO[Http.Http, Option[Place]] =
    val qEnc = URLEncoder.encode(q, "UTF-8")
    Http
      .request(
        method = "GET",
        url =
          s"""https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${qEnc}""",
        headers =
          Map("User-Agent" -> "bot", "Accept" -> "application/json"),
        body = None
      )
      .flatMap { case Response(200, _, body) =>
        body.fromJson[List[PlaceJson]] match
          case Right(pjs) =>
            ZIO.succeed(
              pjs.headOption.map(pj =>
                Place(
                  displayName = pj.display_name,
                  shortName =
                    pj.display_name.split(", ").headOption.getOrElse(q),
                  lat = pj.lat,
                  lon = pj.lon
                )
              )
            )
          case Left(_) =>
            ZIO.succeed(None)
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
  ): RIO[Http.Http, Option[Forecast]] =
    Http
      .request(
        method = "GET",
        url =
          s"""https://api.darksky.net/forecast/${apiKey}/${lat},${lon}""",
        headers =
          Map("User-Agent" -> "bot", "Accept" -> "application/json"),
        body = None
      )
      .flatMap { case Response(200, _, body) =>
        body.fromJson[ForecastJson] match
          case Right(fj) =>
            ZIO.succeed(
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
            )
          case Left(_) =>
            ZIO.succeed(None)
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

  case class AqiParameter(name: String, value: Int, category: String)
  case class Aqi(parameters: List[AqiParameter])

  def findAqi(
      apiKey: String,
      lat: Double,
      lon: Double
  ): RIO[Http.Http, Option[Aqi]] =
    Http
      .request(
        method = "GET",
        url =
          s"""https://www.airnowapi.org/aq/observation/latLong/current/?format=application/json&latitude=${lat}&longitude=${lon}&distance=50&API_KEY=${apiKey}""",
        headers =
          Map("User-Agent" -> "bot", "Accept" -> "application/json"),
        body = None
      )
      .flatMap { case Response(200, _, body) =>
        body.fromJson[List[Observation]] match
          case Right(os) =>
            ZIO.succeed(
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
            )
          case Left(_) =>
            ZIO.succeed(None)
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
  ): RIO[Http.Http, Option[Aqi]] =
    Http
      .request(
        method = "GET",
        url =
          s"""https://www.airnowapi.org/aq/forecast/latLong/?format=application/json&latitude=${lat}&longitude=${lon}&distance=50&API_KEY=${apiKey}""",
        headers =
          Map("User-Agent" -> "bot", "Accept" -> "application/json"),
        body = None
      )
      .flatMap { case Response(200, _, body) =>
        body.fromJson[List[Forecast]] match
          case Right(fs) if fs.length > 0 =>
            ZIO.succeed(
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
            )
          case Right(_) =>
            ZIO.succeed(None)
          case Left(e) =>
            ZIO.succeed(None)
      }

class Weather(darkSkyApiKey: String, airNowApiKey: String)
    extends Producer:

  case class AqiParameter(name: String, value: Int, category: String)

  case class Wx(
      forecast: DarkSky.Forecast,
      aqi: List[AqiParameter]
  )

  private def findWx(
      lat: Double,
      lon: Double
  ): RIO[Http.Http, Option[Wx]] =
    for
      forecastO <- DarkSky.findForecast(
        apiKey = darkSkyApiKey,
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
      forecast <- forecastO
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
      Wx(
        forecast = forecast,
        aqi = aqiMap.values.toList
      )

  private val wx = """^@wx\s+(.+)\s*$""".r

  override def help(): Iterable[Info] =
    Some(Info("@wx", "@wx <location>, e.g. @wx san francisco"))

  override def apply(m: Rx): RIO[Http.Http, Iterable[Tx]] =
    m match
      case Rx(c, _, wx(q)) =>
        OSM.findPlace(q).flatMap {
          case Some(p @ OSM.Place(_, _, lat, lon)) =>
            findWx(lat, lon) flatMap {
              case Some(wx) =>
                ZIO.succeed(
                  Some(
                    Tx(
                      c,
                      (
                        List(
                          f"${p.shortName}: ${wx.forecast.temperature}%.0f° (low ${wx.forecast.temperatureLow}%.0f°, high ${wx.forecast.temperatureHigh}%.0f°)",
                          f"humidity ${wx.forecast.humidity}%.0f%%",
                          f"wind ${wx.forecast.wind}%.0f mph",
                          f"UV ${wx.forecast.uvIndex}"
                        ) ++ wx.aqi.map(p =>
                          s"${p.name} ${p.value} (${p.category})"
                        )
                      ).mkString(", ")
                    )
                  )
                )
              case None =>
                ZIO.succeed(
                  List(Tx(c, s"I can't find the weather for ${q}."))
                )
            }
          case None =>
            ZIO.succeed(List(Tx(c, s"I have no idea where ${q} is.")))
        }
      case _ =>
        ZIO.succeed(None)
