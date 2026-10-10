package sectery.adaptors

import sectery._
import sectery.adaptors.frinkiac._
import sectery.control.Monad
import sectery.control.Monad._
import sectery.effects._

object LiveFrinkiac:

  def apply[F[_]: HttpClient: Monad: Logger](): Frinkiac[F] =
    new Frinkiac:

      override def frinkiac(q: String): F[List[String]] =
        for
          so <- Search.search(q)
          co <- so match {
            case Some(s) =>
              Caption.caption(s.Episode, s.Timestamp)
            case None =>
              summon[Monad[F]].pure(None)
          }
        yield
          val link: List[String] =
            so.toList.map { s =>
              s"https://frinkiac.com/caption/${s.Episode}/${s.Timestamp}"
            }
          val caption: List[String] =
            co.toList.flatMap { c =>
              c.Subtitles.map(_.Content)
            }
          link ++ caption
