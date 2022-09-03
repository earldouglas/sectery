package sectery

import java.net.URI
import java.sql._
import zio._

object Db:

  type Db = Db.Service

  trait Service:
    def apply[A](k: Connection => A): ZIO[Any, Throwable, A]

  lazy val live: ULayer[Service] =
    ZLayer.succeed {
      new Service:
        // for info about the DATABASE_URL env var, see
        // https://devcenter.heroku.com/articles/heroku-postgresql#connecting-in-java
        val dbUri = new URI(sys.env("DATABASE_URL"))
        val username = dbUri.getUserInfo().split(":")(0)
        val password = dbUri.getUserInfo().split(":")(1)
        val dbUrl =
          s"jdbc:mysql://${dbUri.getHost()}:${dbUri.getPort()}${dbUri.getPath()}?sslmode=require"

        override def apply[A](
            k: Connection => A
        ): ZIO[Any, Throwable, A] =
          val conn =
            DriverManager.getConnection(dbUrl, username, password)
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
