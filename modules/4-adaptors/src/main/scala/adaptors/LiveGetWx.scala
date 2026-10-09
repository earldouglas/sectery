package sectery.adaptors

import scala.collection.immutable.TreeMap
import sectery._
import sectery.control.Monad
import sectery.control.Monad._
import sectery.effects._

object LiveGetWx:

  case class AqiParameter(
      name: String,
      value: Int,
      category: String
  )

  def apply[F[_]: HttpClient: Monad: Logger](
      openWeatherMapApiKey: String,
      airNowApiKey: String
  ): GetWx[F] =
    new GetWx:

      private def findWx(
          lat: Double,
          lon: Double
      ): F[Option[String]] =
        for
          currentO <- wx.OpenWeatherMap.findCurrent(
            apiKey = openWeatherMapApiKey,
            lat = lat,
            lon = lon
          )
          aqiObservationO <- wx.AirNowObservation.findAqi(
            apiKey = airNowApiKey,
            lat = lat,
            lon = lon
          )
          aqiForecastO <- wx.AirNowForecast.findAqi(
            apiKey = airNowApiKey,
            lat = lat,
            lon = lon
          )
        yield for
          current <- currentO
          aqiObservation <- aqiObservationO
          aqiForecast <- aqiForecastO
        yield
          var aqiMap: TreeMap[String, AqiParameter] = TreeMap.empty
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
        wx.OSM.findPlace(q).flatMap {
          case Some(p @ wx.OSM.Place(_, _, lat, lon)) =>
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
