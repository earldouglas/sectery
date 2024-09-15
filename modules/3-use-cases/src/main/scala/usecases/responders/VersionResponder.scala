package sectery.usecases.responders

import sectery.usecases.BuildInfo
import sectery.control.Monad
import sectery.domain.entities._
import sectery.usecases.Responder

class VersionResponder[F[_]: Monad] extends Responder[F]:

  override def name = "@version"

  override def usage = "@version"

  override def respondToMessage(rx: Rx) =
    rx.message match
      case "@version" =>
        summon[Monad[F]].pure(
          List(
            Tx(
              service = rx.service,
              channel = rx.channel,
              thread = rx.thread,
              message = s"${BuildInfo.version}"
            )
          )
        )
      case _ =>
        summon[Monad[F]].pure(Nil)
