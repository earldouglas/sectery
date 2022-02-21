package sectery

import org.slf4j.LoggerFactory
import zio.ZIO

object Runtime:

  def catchAndLog[R, E, A](
      z: ZIO[R, E, A],
      orElse: => A
  ): ZIO[R, Nothing, A] =
    z.catchAllCause { cause =>
      LoggerFactory
        .getLogger(this.getClass())
        .error(cause.prettyPrint)
      ZIO.succeed(orElse)
    }

  def catchAndLog[R, E](z: ZIO[R, E, Any]): ZIO[R, Nothing, Unit] =
    catchAndLog(z.map(_ => ()), ())
