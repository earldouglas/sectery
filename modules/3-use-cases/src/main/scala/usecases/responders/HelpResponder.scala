package sectery.usecases.responders

import sectery.control.Monad
import sectery.domain.entities._
import sectery.usecases.Responder

class HelpResponder[F[_]: Monad](_responders: List[Responder[F]])
    extends Responder[F]:

  override def name = "@help"

  override def usage = "@help"

  private val responders: List[Responder[F]] =
    this :: _responders

  private val helpMessage: String =
    responders
      .map(p => p.name)
      .sorted
      .mkString(", ")

  private val usageMap: Map[String, String] =
    responders
      .map(i => i.name -> i.usage)
      .toMap

  private val help = """^@help\s+(.+)\s*$""".r

  override def respondToMessage(rx: Rx) =
    rx match
      case Rx(c, _, "@help") =>
        summon[Monad[F]].pure(List(Tx(c, helpMessage)))
      case Rx(c, _, help(name)) =>
        usageMap.get(name) match
          case Some(usage) =>
            summon[Monad[F]].pure(List(Tx(c, s"Usage: ${usage}")))
          case None =>
            summon[Monad[F]].pure(
              List(Tx(c, s"I don't know anything about ${name}"))
            )
      case _ =>
        summon[Monad[F]].pure(Nil)
