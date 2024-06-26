package sectery.usecases.responders

import sectery.control.Monad
import sectery.control.Monad._
import sectery.control.Traversable
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases.Responder

class StockResponder[F[_]: Monad: Stock: Traversable: GetConfig]
    extends Responder[F]:

  override def name = "@stock"

  override def usage =
    "@stock [symbol1] [symbol2] ... [symbolN], e.g. @stock GME"

  private val stock = """^@stock\s+([^\s]+([\s,]+([^\s]+))*)$""".r

  private def getQuotes(symbolsString: String): F[List[String]] =
    val symbols: List[String] =
      symbolsString
        .split("""[\s,]+""")
        .toList
        .map(_.trim())
        .map(_.toUpperCase())
        .toSet
        .toList

    summon[Monad[F]]
      .pure(symbols)
      .flatMap { symbols =>
        summon[Traversable[F]]
          .traverse(symbols) { symbol =>
            summon[Stock[F]]
              .getQuote(symbol)
              .map {
                case Some(quote) => quote
                case None        => s"${symbol}: stonk not found"
              }
          }
      }

  override def respondToMessage(rx: Rx) =
    rx.message match
      case stock(symbolsString, _, _) =>
        getQuotes(symbolsString)
          .map { quotes =>
            quotes.map { quote =>
              Tx(
                service = rx.service,
                channel = rx.channel,
                thread = rx.thread,
                message = quote
              )
            }
          }
      case "@stock" =>
        for
          so <- summon[GetConfig[F]].getConfig(rx.nick, "stock")
          txs <- so match
            case Some(symbolsString) =>
              getQuotes(symbolsString)
                .map { quotes =>
                  quotes.map { quote =>
                    Tx(
                      service = rx.service,
                      channel = rx.channel,
                      thread = rx.thread,
                      message = quote
                    )
                  }
                }
            case None =>
              val message =
                s"${rx.nick}: Set default symbols with `@set stock <symbol1> [symbol2] ... [symbolN]`"
              summon[Monad[F]].pure(
                List(
                  Tx(
                    service = rx.service,
                    channel = rx.channel,
                    thread = rx.thread,
                    message = message
                  )
                )
              )
        yield txs

      case _ =>
        summon[Monad[F]].pure(Nil)
