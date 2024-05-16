package sectery.usecases.responders

import sectery.control.Monad
import sectery.control.Monad._
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases.Responder

class EvalResponder[F[_]: Monad: Eval] extends Responder[F]:

  override def name = "@eval"

  override def usage = "@eval <expression>, e.g. @eval 6 * 7"

  private val eval = """^@eval\s+(.+)\s*$""".r

  override def respondToMessage(rx: Rx) =
    rx match
      case Rx(c, _, eval(expr)) =>
        summon[Eval[F]]
          .eval(expr)
          .map {
            case Right(x) => x
            case Left(x)  => x
          }
          .map { message =>
            List(Tx(c, message))
          }
      case _ =>
        summon[Monad[F]].pure(Nil)
