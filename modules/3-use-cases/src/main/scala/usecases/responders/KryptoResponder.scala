package sectery.usecases.responders

import net.objecthunter.exp4j.ExpressionBuilder
import sectery.control.Monad
import sectery.control.Monad._
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases.Responder

class KryptoResponder[F[_]: Monad: Krypto] extends Responder[F]:

  override def name = "@krypto"

  override def usage = "@krypto"

  private val krypto = """^@krypto\s+(.+)$""".r

  private def tries(count: Int): String =
    if count == 1 then s"${count} try" else s"${count} tries"

  def calc(expr: String): Option[Double] =
    val allowed = """^[0-9()+*\s-]*$""".r
    expr match
      case allowed() =>
        val expression = new ExpressionBuilder(expr).build()
        Option(expression.evaluate())
      case _ => None

  def show(game: Krypto.Game): String =
    s"Cards: ${game.cards.toList.mkString("[", ", ", "]")}.  Objective: ${game.objective}.  ${tries(game.guessCount)} so far."

  override def respondToMessage(rx: Rx) =
    rx match

      case Rx(c, _, "@krypto") =>
        summon[Krypto[F]]
          .getOrStartGame(c)
          .map(game => List(Tx(c, show(game))))

      case Rx(c, _, krypto(guess)) =>
        for
          game <- summon[Krypto[F]].getOrStartGame(c)
          tx <-
            calc(guess) match
              case Some(result) =>
                if (game.objective == result) then
                  summon[Krypto[F]]
                    .deleteGame(c)
                    .map(_ =>
                      Tx(
                        c,
                        s"Krypto!  Got it in ${tries(game.guessCount + 1)}."
                      )
                    )
                else
                  val newGuessCount = game.guessCount + 1
                  summon[Krypto[F]]
                    .setGuessCount(c, newGuessCount)
                    .map { _ =>
                      Tx(
                        c,
                        s"Try again.  ${tries(newGuessCount)} so far."
                      )
                    }
              case None =>
                summon[Monad[F]].pure(Tx(c, "Can't parse that."))
        yield List(tx)

      case _ =>
        summon[Monad[F]].pure(Nil)
