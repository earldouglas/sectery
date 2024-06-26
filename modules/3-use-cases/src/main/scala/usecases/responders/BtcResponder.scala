package sectery.usecases.responders

import java.text.NumberFormat
import java.util.Locale
import sectery.control.Monad
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases.Responder

class BtcResponder[F[_]: Monad: Btc] extends Responder[F]:

  override def name = "@btc"

  override def usage = "@btc"

  override def respondToMessage(rx: Rx) =
    rx.message match
      case "@btc" =>
        summon[Monad[F]]
          .flatMap(summon[Btc[F]].toUsd()) {
            case Some(rate) =>
              val message =
                s"${NumberFormat.getCurrencyInstance(Locale.US).format(rate)}"
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
            case None =>
              summon[Monad[F]].pure(Nil)
          }
      case _ =>
        summon[Monad[F]].pure(Nil)
