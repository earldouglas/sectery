package sectery.usecases

import sectery.control.Monad._
import sectery.control._
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases.announcers.AutoquoteAnnouncer

class Announcers[
    F[_] //
    : Autoquote //
    : Monad //
    : Now //
    : [F[_]] =>> Enqueue[F, Tx] //
    : Traversable //
]:

  private val announcers: List[Announcer[F]] =
    List(
      new AutoquoteAnnouncer
    )

  def announce(): F[Unit] =
    summon[Traversable[F]]
      .traverse(announcers)(_.announce())
      .map(_.flatten)
      .flatMap { txs =>
        summon[Traversable[F]]
          .traverse(txs) { tx =>
            summon[Enqueue[F, Tx]]
              .apply(tx)
          }
          .map { xs =>
            xs.fold(())((_, _) => ())
          }
      }
