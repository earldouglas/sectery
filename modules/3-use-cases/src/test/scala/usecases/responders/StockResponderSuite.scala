package sectery.usecases.responders

import munit.FunSuite
import sectery.domain.entities._
import sectery.effects._
import sectery.effects.id.Id
import sectery.effects.id.given

class StockResponderSuite extends FunSuite:

  test("@stock <symbol> produces quote") {

    given getQuote: Stock[Id] with
      override def getQuote(symbol: String): Id[Option[String]] =
        symbol match {
          case "VOO" =>
            Some("VOO: 390.09 +0.68 (+0.17%)")
        }

    given getConfig: GetConfig[Id] with
      override def getConfig(
          nick: String,
          key: String
      ): Id[Option[String]] =
        None

    val obtained: List[Tx] =
      new StockResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@stock VOO")
        )

    val expected: List[Tx] =
      List(
        Tx(
          "irc",
          "#foo",
          None,
          "VOO: 390.09 +0.68 (+0.17%)"
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("@stock produces help with no user-configured symbol") {

    given getQuote: Stock[Id] with
      override def getQuote(symbol: String): Id[Option[String]] = ???

    given getConfig: GetConfig[Id] with
      override def getConfig(
          nick: String,
          key: String
      ): Id[Option[String]] =
        None

    val obtained: List[Tx] =
      new StockResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@stock")
        )

    val expected: List[Tx] =
      List(
        Tx(
          "irc",
          "#foo",
          None,
          "bar: Set default symbols with `@set stock <symbol1> [symbol2] ... [symbolN]`"
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("@stock produces quote with user-configured symbol") {

    given getQuote: Stock[Id] with
      override def getQuote(symbol: String): Id[Option[String]] =
        symbol match {
          case "VOO" =>
            Some("VOO: 390.09 +0.68 (+0.17%)")
        }

    given getConfig: GetConfig[Id] with
      override def getConfig(
          nick: String,
          key: String
      ): Id[Option[String]] =
        (nick, key) match {
          case ("bar", "stock") =>
            Some("VOO")
        }

    val obtained: List[Tx] =
      new StockResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@stock")
        )

    val expected: List[Tx] =
      List(
        Tx(
          "irc",
          "#foo",
          None,
          "VOO: 390.09 +0.68 (+0.17%)"
        )
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }

  test("@stock FOO produces unknown symbol error") {

    given getQuote: Stock[Id] with
      override def getQuote(symbol: String): Id[Option[String]] =
        None

    given getConfig: GetConfig[Id] with
      override def getConfig(
          nick: String,
          key: String
      ): Id[Option[String]] =
        None

    val obtained: List[Tx] =
      new StockResponder[Id]
        .respondToMessage(
          Rx("irc", "#foo", None, "bar", "@stock FOO")
        )

    val expected: List[Tx] =
      List(
        Tx("irc", "#foo", None, "FOO: stonk not found")
      )

    assertEquals(
      obtained = obtained,
      expected = expected
    )
  }
