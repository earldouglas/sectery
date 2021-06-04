package sectery.producers

import java.net.URLEncoder
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.MonadicJValue.jvalueToMonadic
import org.json4s.native.JsonMethods._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import scala.collection.JavaConverters._
import sectery.Http
import sectery.Producer
import sectery.Response
import sectery.Rx
import sectery.Tx
import zio.clock.Clock
import zio.Has
import zio.URIO
import zio.ZIO

class Weather(darkSkyApiKey: String) extends Producer:

  case class Place(displayName: String, lat: Double, lon: Double)
  case class Wx(temperature: Double, humidity: Double, wind: Double, gusts: Double, uvIndex: Int)

  private def findPlace(q: String): URIO[Http.Http, Option[Place]] =
    Http.request(
      method = "GET",
      url = s"""https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${URLEncoder.encode(q, "UTF-8")}""",
      headers = Map("User-Agent" -> "bot", "Accept" -> "application/json"),
      body = None
    )
    .flatMap {
      case Response(200, _, body) =>
        ZIO.effect {
          val json = parse(body)
          ( 
            json(0) \ "display_name",
            json(0) \ "lat",
            json(0) \ "lon"
          ) match
            case (JString(displayName), JString(lat), JString(lon)) =>
              Some(Place(displayName = displayName, lat = lat.toDouble, lon = lon.toDouble))
            case _ =>
              None
        }
    }
    .catchAll { e =>
      LoggerFactory.getLogger(this.getClass()).error("caught exception", e)
      ZIO.effectTotal(None)
    }

  private def findWx(lat: Double, lon: Double): URIO[Http.Http, Option[Wx]] =
    Http.request(
      method = "GET",
      url = s"""https://api.darksky.net/forecast/${darkSkyApiKey}/${lat},${lon}""",
      headers = Map("User-Agent" -> "bot", "Accept" -> "application/json"),
      body = None
    )
    .flatMap {
      case Response(200, _, body) =>
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
                Wx(
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
      LoggerFactory.getLogger(this.getClass()).error("caught exception", e)
      ZIO.effectTotal(None)
    }

  private val wx = """^@wx\s+(.+)\s*$""".r

  override def help(): Iterable[String] =
    Some("@wx")

  override def apply(m: Rx): URIO[Http.Http, Iterable[Tx]] =
    m match
      case Rx(c, _, wx(q)) =>
        findPlace(q).flatMap {
          case Some(p@Place(_, lat, lon)) =>
            findWx(lat, lon) flatMap {
              case Some(wx) =>
                ZIO.succeed(Some(Tx(c, f"${p.displayName}: temperature ${wx.temperature}%.0f°, humidity ${wx.humidity}%.1f%%, wind ${wx.wind}%.1f mph, UV index ${wx.uvIndex}")))
              case None =>
                ZIO.succeed(List(Tx(c, s"I can't find the weather for ${q}.")))
            }
          case None =>
            ZIO.succeed(List(Tx(c, s"I have no idea where ${q} is.")))
        }
      case _ =>
        ZIO.effectTotal(None)
