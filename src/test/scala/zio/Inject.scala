package zio

object Inject:

  implicit class Inject2[R0, R1, E, A](z: ZIO[Has[R0] with Has[R1], E, A])(implicit t: Tag[Has[R1]]):
    def inject(r0: ULayer[Has[R0]]): ZIO[Has[R1], E, A] =
      ZIO.fromFunctionM { r =>
        z.provideLayer(r0 ++ ZLayer.succeedMany(r))
      }

  implicit class Inject3[R0, R1, R2, E, A](z: ZIO[Has[R0] with Has[R1] with Has[R2], E, A])(implicit t1: Tag[Has[R1]], t2: Tag[Has[R2]]):
    def inject(r0: ULayer[Has[R0]], r1: ULayer[Has[R1]]): ZIO[Has[R2], E, A] =
      ZIO.fromFunctionM { r =>
        z.provideLayer(r0 ++ r1 ++ ZLayer.succeedMany(r))
      }
