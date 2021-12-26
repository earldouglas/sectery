package zio

object Inject:

  implicit class Inject2[R0, R1, E, A](
      z: ZIO[R0 with R1, E, A]
  )(implicit t: Tag[R1]):
    def inject_(r0: ULayer[R0]): ZIO[R1, E, A] =
      ZIO.environmentWithZIO { r =>
        z.provideLayer(r0 ++ ZLayer.succeedEnvironment(r))
      }

  implicit class Inject3[R0, R1, R2, E, A](
      z: ZIO[R0 with R1 with R2, E, A]
  )(implicit t1: Tag[R1], t2: Tag[R2]):
    def inject_(
        r0: ULayer[R0],
        r1: ULayer[R1]
    ): ZIO[R2, E, A] =
      ZIO.environmentWithZIO { r =>
        z.provideLayer(r0 ++ r1 ++ ZLayer.succeedEnvironment(r))
      }

  implicit class Inject4[R0, R1, R2, R3, E, A](
      z: ZIO[R0 with R1 with R2 with R3, E, A]
  )(implicit t1: Tag[R1], t2: Tag[R2], t3: Tag[R3]):
    def inject_(
        r0: ULayer[R0],
        r1: ULayer[R1],
        r2: ULayer[R2]
    ): ZIO[R3, E, A] =
      ZIO.environmentWithZIO { r =>
        z.provideLayer(r0 ++ r1 ++ r2 ++ ZLayer.succeedEnvironment(r))
      }
