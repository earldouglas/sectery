package sectery.producers

import java.net.URLEncoder
import org.json4s.JsonDSL._
import org.json4s.MonadicJValue.jvalueToMonadic
import org.json4s._
import org.json4s.native.JsonMethods._
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
import zio.Has
import zio.URIO
import zio.ZIO
import zio.clock.Clock

object OSM:

  case class Place(displayName: String, lat: Double, lon: Double)

  def findPlace(q: String): URIO[Http.Http, Option[Place]] =
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
        ZIO.effect {
          val json = parse(body)
          (
            json(0) \ "display_name",
            json(0) \ "lat",
            json(0) \ "lon"
          ) match
            case (JString(displayName), JString(lat), JString(lon)) =>
              Some(
                Place(
                  displayName = displayName,
                  lat = lat.toDouble,
                  lon = lon.toDouble
                )
              )
            case _ =>
              None
        }
      }
      .catchAll { e =>
        LoggerFactory
          .getLogger(this.getClass())
          .error("caught exception", e)
        ZIO.effectTotal(None)
      }

object DarkSky:

  case class Forecast(
      temperature: Double,
      humidity: Double,
      wind: Double,
      gusts: Double,
      uvIndex: Int
  )

  def findForecast(
      apiKey: String,
      lat: Double,
      lon: Double
  ): URIO[Http.Http, Option[Forecast]] =
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
        ZIO.effect {
          val json = parse(body)
          (
            json \ "currently" \ "temperature",
            json \ "currently" \ "humidity",
            json \ "currently" \ "windSpeed",
            json \ "currently" \ "windGust",
            json \ "currently" \ "uvIndex"
          ) match
            case (
                  JDouble(temperature),
                  JDouble(humidity),
                  JDouble(windSpeed),
                  JDouble(windGust),
                  JInt(uvIndex)
                ) =>
              Some(
                Forecast(
                  temperature = temperature,
                  humidity = humidity,
                  wind = windSpeed,
                  gusts = windGust,
                  uvIndex = uvIndex.toInt
                )
              )
            case _ =>
              None
        }
      }
      .catchAll { e =>
        LoggerFactory
          .getLogger(this.getClass())
          .error("caught exception", e)
        ZIO.effectTotal(None)
      }

object AirNowObservation:

  case class AqiParameter(name: String, value: Int, category: String)
  case class Aqi(parameters: List[AqiParameter])

  private def parseAqiParameter(v: JValue): Option[AqiParameter] =
    (
      v \ "ParameterName",
      v \ "AQI",
      v \ "Category" \ "Name"
    ) match
      case (JString(name), JInt(value), JString(category)) =>
        Some(
          AqiParameter(
            name = name,
            value = value.toInt,
            category = category
          )
        )
      case _ =>
        None

  private def parseAqi(v: JValue): Option[Aqi] =
    v match
      case JArray(xs) =>
        Some(Aqi(xs.flatMap(parseAqiParameter)))
      case _ =>
        None

  def findAqi(
      apiKey: String,
      lat: Double,
      lon: Double
  ): URIO[Http.Http, Option[Aqi]] =
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
        ZIO.effect(parseAqi(parse(body)))
      }
      .catchAll { e =>
        LoggerFactory
          .getLogger(this.getClass())
          .error("caught exception", e)
        ZIO.effectTotal(None)
      }

object AirNowForecast:

  case class AqiParameter(
      name: String,
      date: String,
      value: Int,
      category: String
  )
  case class Aqi(parameters: List[AqiParameter])

  private def parseAqiParameter(v: JValue): Option[AqiParameter] =
    (
      v \ "DateForecast",
      v \ "ParameterName",
      v \ "AQI",
      v \ "Category" \ "Name"
    ) match
      case (
            JString(date),
            JString(name),
            JInt(value),
            JString(category)
          ) =>
        Some(
          AqiParameter(
            name = name,
            date = date,
            value = value.toInt,
            category = category
          )
        )
      case _ =>
        None

  private def parseAqi(v: JValue): Option[Aqi] =
    v match
      case JArray(xs) =>
        Some(Aqi(xs.flatMap(parseAqiParameter)))
      case _ =>
        None

  def findAqi(
      apiKey: String,
      lat: Double,
      lon: Double
  ): URIO[Http.Http, Option[Aqi]] =
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
        ZIO.effect(parseAqi(parse(body)))
      }
      .catchAll { e =>
        LoggerFactory
          .getLogger(this.getClass())
          .error("caught exception", e)
        ZIO.effectTotal(None)
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
  ): URIO[Http.Http, Option[Wx]] =
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
        if (!aqiMap.contains(p.name)) {
          aqiMap = aqiMap + (p.name -> AqiParameter(
            name = p.name,
            value = p.value,
            category = p.category
          ))
        }
      }
      aqiForecast.parameters.sortBy(_.date).foreach { case p =>
        if (!aqiMap.contains(p.name)) {
          aqiMap = aqiMap + (p.name -> AqiParameter(
            name = p.name,
            value = p.value,
            category = p.category
          ))
        }
      }
      Wx(
        forecast = forecast,
        aqi = aqiMap.values.toList
      )

  private val wx = """^@wx\s+(.+)\s*$""".r

  override def help(): Iterable[Info] =
    Some(Info("@wx", "@wx <location>, e.g. @wx san francisco"))

  override def apply(m: Rx): URIO[Http.Http, Iterable[Tx]] =
    m match
      case Rx(c, _, wx(q)) =>
        OSM.findPlace(q).flatMap {
          case Some(p @ OSM.Place(_, lat, lon)) =>
            findWx(lat, lon) flatMap {
              case Some(wx) =>
                ZIO.succeed(
                  Some(
                    Tx(
                      c,
                      (
                        List(
                          f"${p.displayName}: temperature ${wx.forecast.temperature}%.0f°",
                          f"humidity ${wx.forecast.humidity}%.1f%%",
                          f"wind ${wx.forecast.wind}%.1f mph",
                          f"UV index ${wx.forecast.uvIndex}"
                        ) ++ wx.aqi.map(p =>
                          s"${p.name}: ${p.value}/${p.category}"
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
        ZIO.effectTotal(None)
