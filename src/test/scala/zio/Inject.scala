package zio

object Inject:

  implicit class Inject2[R0, R1, E, A](
      z: ZIO[Has[R0] with Has[R1], E, A]
  )(implicit t: Tag[Has[R1]]):
    def inject_(r0: ULayer[Has[R0]]): ZIO[Has[R1], E, A] =
      ZIO.accessZIO { r =>
        z.provideLayer(r0 ++ ZLayer.succeedMany(r))
      }

  implicit class Inject3[R0, R1, R2, E, A](
      z: ZIO[Has[R0] with Has[R1] with Has[R2], E, A]
  )(implicit t1: Tag[Has[R1]], t2: Tag[Has[R2]]):
    def inject_(
        r0: ULayer[Has[R0]],
        r1: ULayer[Has[R1]]
    ): ZIO[Has[R2], E, A] =
      ZIO.accessZIO { r =>
        z.provideLayer(r0 ++ r1 ++ ZLayer.succeedMany(r))
      }

  implicit class Inject4[R0, R1, R2, R3, E, A](
      z: ZIO[Has[R0] with Has[R1] with Has[R2] with Has[R3], E, A]
  )(implicit t1: Tag[Has[R1]], t2: Tag[Has[R2]], t3: Tag[Has[R3]]):
    def inject_(
        r0: ULayer[Has[R0]],
        r1: ULayer[Has[R1]],
        r2: ULayer[Has[R2]]
    ): ZIO[Has[R3], E, A] =
      ZIO.accessZIO { r =>
        z.provideLayer(r0 ++ r1 ++ r2 ++ ZLayer.succeedMany(r))
      }
