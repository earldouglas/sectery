package sectery.usecases.responders

import sectery.control.Monad
import sectery.control.Monad._
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases.Responder

class WxResponder[F[_]: GetWx: GetConfig: Monad] extends Responder[F]:

  override def name = "@wx"

  override def usage = "@wx [location], e.g. @wx san francisco"

  private val wx = """^@wx(\s+(.+)\s*)?$""".r

  override def respondToMessage(rx: Rx) =
    rx.message match
      case "@wx" =>
        for
          lo <- summon[GetConfig[F]].getConfig(rx.nick, "wx")
          txs <- lo match
            case Some(l) =>
              summon[GetWx[F]]
                .getWx(l)
                .map(message =>
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
              val message =
                s"${rx.nick}: Set default location with `@set wx <location>`"
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
      case wx(_, q) =>
        summon[GetWx[F]]
          .getWx(q)
          .map(message =>
            List(
              Tx(
                service = rx.service,
                channel = rx.channel,
                thread = rx.thread,
                message = message
              )
            )
          )
      case _ =>
        summon[Monad[F]].pure(Nil)
