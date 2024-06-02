package sectery.usecases.responders

import net.objecthunter.exp4j.ExpressionBuilder
import sectery.control.Monad
import sectery.control.Monad._
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases.Responder

class KryptoResponder[F[_]: Monad: Krypto] extends Responder[F]:

  override def name = "@krypto"

  override def usage = "@krypto [guess]"

  private val krypto = """^@krypto\s+(.+)$""".r

  private def tries(count: Int): String =
    if count == 1 then s"${count} try" else s"${count} tries"

  def checkCards(game: Krypto.Game, expr: String): Boolean =

    val guessCards: List[Int] =
      expr
        .split("[^0-9]")
        .toList
        .filter(_.length > 0)
        .map(_.toInt)
        .sorted

    val gameCards: List[Int] =
      List(
        game.cards._1,
        game.cards._2,
        game.cards._3,
        game.cards._4,
        game.cards._5
      ).sorted

    gameCards == guessCards

  def calc(expr: String): Option[Double] =
    val allowed = """^[0-9()+*/\s-]*$""".r
    expr match
      case allowed() =>
        try
          val expression = new ExpressionBuilder(expr).build()
          Option(expression.evaluate())
        catch case e: IllegalArgumentException => None
      case _ => None

  def show(game: Krypto.Game): String =
    s"Cards: ${game.cards.toList.mkString("[", ", ", "]")}.  Objective: ${game.objective}.  ${tries(game.guessCount)} so far."

  override def respondToMessage(rx: Rx) =
    rx.message match

      case "@krypto" =>
        summon[Krypto[F]]
          .getOrStartGame(rx.service, rx.channel)
          .map(game =>
            List(
              Tx(
                service = rx.service,
                channel = rx.channel,
                thread = rx.thread,
                message = show(game)
              )
            )
          )

      case krypto(guess) =>
        summon[Krypto[F]]
          .getOrStartGame(rx.service, rx.channel)
          .flatMap { game =>
            calc(guess) match
              case Some(result) =>
                checkCards(game, guess) match
                  case false =>
                    summon[Monad[F]].pure(
                      List(
                        Tx(
                          service = rx.service,
                          channel = rx.channel,
                          thread = rx.thread,
                          message = "Gotta play the cards as dealt."
                        )
                      )
                    )
                  case true =>
                    if (game.objective == result) then
                      summon[Krypto[F]]
                        .deleteGame(rx.service, rx.channel)
                        .map { _ =>
                          val message =
                            s"Krypto!  Got it in ${tries(game.guessCount + 1)}."
                          List(
                            Tx(
                              service = rx.service,
                              channel = rx.channel,
                              thread = rx.thread,
                              message = message
                            )
                          )
                        }
                    else
                      val newGuessCount = game.guessCount + 1
                      summon[Krypto[F]]
                        .setGuessCount(
                          rx.service,
                          rx.channel,
                          newGuessCount
                        )
                        .map { _ =>
                          val message =
                            s"Try again.  ${tries(newGuessCount)} so far."
                          List(
                            Tx(
                              service = rx.service,
                              channel = rx.channel,
                              thread = rx.thread,
                              message = message
                            )
                          )
                        }
              case None =>
                summon[Monad[F]].pure(
                  List(
                    Tx(
                      service = rx.service,
                      channel = rx.channel,
                      thread = rx.thread,
                      message = "Can't parse that."
                    )
                  )
                )
          }

      case _ =>
        summon[Monad[F]].pure(Nil)
