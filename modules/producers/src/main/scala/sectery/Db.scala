package sectery

import java.sql.Connection
import java.sql.DriverManager
import zio.ZIO
import zio.ZLayer

object Db:

  type Db = Db.Service

  trait Service:
    def apply[A](k: Connection => A): ZIO[Any, Throwable, A]

  lazy val live: ZLayer[Any, Nothing, Service] =
    ZLayer.succeed {
      new Service:
        val dbUrl = sys.env("DATABASE_URL")

        override def apply[A](
            k: Connection => A
        ): ZIO[Any, Throwable, A] =
          val conn = DriverManager.getConnection(dbUrl)
          ZIO
            .attempt {
              conn.setAutoCommit(false)
              val result: A = k(conn)
              conn.commit()
              conn.close()
              result
            }
            .catchAll { e =>
              for
                _ <- ZIO.logError("rolling back transaction")
                _ = conn.rollback()
                _ = conn.close()
                f <- ZIO.fail(e)
              yield f
            }
    }

  def apply[A](k: Connection => A): ZIO[Db, Throwable, A] =
    ZIO.environmentWithZIO(_.get.apply(k))
