package sectery.usecases.responders

import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class WxResponderSuite extends FunSuite:

  test("@wx <location> produces weather in specified location") {

    given getWx: GetWx[Id] with
      override def getWx(location: String): Id[String] =
        location match {
          case "san francisco" =>
            "San Francisco: 44°, hum 65%, wnd 10 mph, few clouds, uv 1, o3 15, pm2.5 25, pm10 10"
        }

    given getConfig: GetConfig[Id] with
      override def getConfig(
          nick: String,
          key: String
      ): Id[Option[String]] =
        None

    val obtained: List[Tx] =
      new WxResponder[Id]
        .respondToMessage(Rx("#foo", "bar", "@wx san francisco"))

    val expected: List[Tx] =
      List(
        Tx(
          "#foo",
          "San Francisco: 44°, hum 65%, wnd 10 mph, few clouds, uv 1, o3 15, pm2.5 25, pm10 10"
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("@wx produces help with no user-configured location") {

    given getWx: GetWx[Id] with
      override def getWx(location: String): Id[String] =
        location match {
          case "san francisco" =>
            "bar: Set default location with `@set wx <location>`"
        }

    given getConfig: GetConfig[Id] with
      override def getConfig(
          nick: String,
          key: String
      ): Id[Option[String]] =
        None

    val obtained: List[Tx] =
      new WxResponder[Id].respondToMessage(Rx("#foo", "bar", "@wx"))

    val expected: List[Tx] =
      List(
        Tx(
          "#foo",
          "bar: Set default location with `@set wx <location>`"
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("@wx produces weather in user-configured location") {

    given getWx: GetWx[Id] with
      override def getWx(location: String): Id[String] =
        location match {
          case "san francisco" =>
            "San Francisco: 44°, hum 65%, wnd 10 mph, few clouds, uv 1, o3 15, pm2.5 25, pm10 10"
        }

    given getConfig: GetConfig[Id] with
      override def getConfig(
          nick: String,
          key: String
      ): Id[Option[String]] =
        (nick, key) match {
          case ("bar", "wx") =>
            Some("san francisco")
        }

    val obtained: List[Tx] =
      new WxResponder[Id].respondToMessage(Rx("#foo", "bar", "@wx"))

    val expected: List[Tx] =
      List(
        Tx(
          "#foo",
          "San Francisco: 44°, hum 65%, wnd 10 mph, few clouds, uv 1, o3 15, pm2.5 25, pm10 10"
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }
