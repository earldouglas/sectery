package sectery.producers

import java.net.URLEncoder
import scala.jdk.CollectionConverters._
import sectery.Db
import sectery.Http
import sectery.Producer
import sectery.Response
import sectery.Rx
import sectery.Tx
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

  def findPlace(q: String): ZIO[Http.Http, Throwable, Option[Place]] =
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
  ): ZIO[Http.Http, Throwable, Option[Forecast]] =
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

object OpenWeatherMap:

  case class OneCall(
      current: Current
  )

  object OneCall:
    implicit val decoder: JsonDecoder[OneCall] =
      DeriveJsonDecoder.gen[OneCall]

  case class Current(
      temp: Float,
      humidity: Float,
      wind_speed: Float,
      uvi: Float
  )

  object Current:
    implicit val decoder: JsonDecoder[Current] =
      DeriveJsonDecoder.gen[Current]

  def findCurrent(
      apiKey: String,
      lat: Double,
      lon: Double
  ): ZIO[Http.Http, Throwable, Option[Current]] =
    Http
      .request(
        method = "GET",
        url =
          s"""https://api.openweathermap.org/data/3.0/onecall?lat=${lat}&lon=${lon}&exclude=minutely,hourly,daily,alerts&units=imperial&appid=${apiKey}""",
        headers =
          Map("User-Agent" -> "bot", "Accept" -> "application/json"),
        body = None
      )
      .flatMap { case Response(200, _, body) =>
        body.fromJson[OneCall] match
          case Right(x) => ZIO.succeed(Some(x.current))
          case Left(e)  => ZIO.succeed(None)
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
  ): ZIO[Http.Http, Throwable, Option[Aqi]] =
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
  ): ZIO[Http.Http, Throwable, Option[Aqi]] =
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

class Weather(openWeatherMapApiKey: String, airNowApiKey: String)
    extends Producer:

  case class AqiParameter(name: String, value: Int, category: String)

  case class Wx(
      current: OpenWeatherMap.Current,
      aqi: List[AqiParameter]
  )

  private def findWx(
      lat: Double,
      lon: Double
  ): ZIO[Http.Http, Throwable, Option[Wx]] =
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
      Wx(
        current = current,
        aqi = aqiMap.values.toList
      )

  private val wx = """^@wx(\s+(.+)\s*)?$""".r

  override def help(): Iterable[Info] =
    Some(Info("@wx", "@wx [location], e.g. @wx san francisco"))

  private def findWx(
      c: String,
      q: String
  ): ZIO[Http.Http, Throwable, Iterable[Tx]] =
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
                      f"${p.shortName}: ${wx.current.temp}%.0f°",
                      f"humidity ${wx.current.humidity}%.0f%%",
                      f"wind ${wx.current.wind_speed}%.0f mph",
                      f"UV ${wx.current.uvi.toInt}"
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

  override def apply(
      m: Rx
  ): ZIO[Db.Db with Http.Http, Throwable, Iterable[Tx]] =
    m match
      case Rx(c, nick, "@wx") =>
        for
          lo <- Config.getConfig(nick, "wx")
          txs <- lo match
            case Some(l) =>
              findWx(c, l)
            case None =>
              ZIO.succeed(
                List(
                  Tx(
                    c,
                    s"${nick}: Set default location with `@set wx <location>`"
                  )
                )
              )
        yield txs
      case Rx(c, _, wx(_, q)) =>
        findWx(c, q)
      case _ =>
        ZIO.succeed(None)
