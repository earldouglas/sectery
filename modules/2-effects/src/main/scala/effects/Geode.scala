package sectery.effects

trait Geode[F[_]]:
  def geode(ip: String): F[Either[String, Geode.Location]]

object Geode:

  /*
  8.8.4.4
  {
      "city": "Westfield",
      "continent": "North America",
      "continentCode": "NA",
      "countryCode": "US",
      "countryName": "United States",
      "latitude": 42.1293,
      "longitude": -72.7522,
      "postalCode": "01085",
      "region": "MA",
      "regionName": "Massachusetts"
  }
   */

  case class Location(
      city: Option[String],
      continent: Option[String],
      continentCode: Option[String],
      countryCode: Option[String],
      countryName: Option[String],
      latitude: Option[Float],
      longitude: Option[Float],
      postalCode: Option[String],
      region: Option[String],
      regionName: Option[String]
  ):

    override def toString(): String =
      List(
        city.map(x => (s"city: ${x}")),
        continent.map(x => (s"continent: ${x}")),
        continentCode.map(x => (s"continent code: ${x}")),
        countryCode.map(x => (s"country code: ${x}")),
        countryName.map(x => (s"country name: ${x}")),
        latitude.map(x => (s"latitude: ${x}")),
        longitude.map(x => (s"longitude: ${x}")),
        postalCode.map(x => (s"postal code: ${x}")),
        region.map(x => (s"region: ${x}")),
        regionName.map(x => (s"region name: ${x}"))
      ).flatten.mkString(", ")
