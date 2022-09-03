package sectery

import zio.ZIO

object Runtime:

  def catchAndLog[R, E, A](
      z: ZIO[R, E, A],
      orElse: => A
  ): ZIO[R, Nothing, A] =
    z.catchAllCause { cause =>
      for _ <- ZIO.logError(cause.prettyPrint)
      yield orElse
    }

  def catchAndLog[R, E](z: ZIO[R, E, Any]): ZIO[R, Nothing, Unit] =
    catchAndLog(z.map(_ => ()), ())
