package sectery.adaptors

import sectery._
import sectery.adaptors.morbotron._
import sectery.control.Monad
import sectery.control.Monad._
import sectery.effects._

object LiveMorbotron:

  def apply[F[_]: HttpClient: Monad: Logger](): Morbotron[F] =
    new Morbotron:

      override def morbotron(q: String): F[List[String]] =
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
              s"https://morbotron.com/caption/${s.Episode}/${s.Timestamp}"
            }
          val caption: List[String] =
            co.toList.flatMap { c =>
              c.Subtitles.map(_.Content)
            }
          link ++ caption
