package sectery

import java.net.URI
import java.sql._
import org.slf4j.LoggerFactory
import zio._

object Db:

  type Db = Has[Db.Service]

  trait Service:
    def apply[A](k: Connection => A): Task[A]

  lazy val live: ULayer[Has[Service]] =
    ZLayer.succeed {
      new Service:
        // for info about the DATABASE_URL env var, see
        // https://devcenter.heroku.com/articles/heroku-postgresql#connecting-in-java
        val dbUri = new URI(sys.env("DATABASE_URL"))
        val username = dbUri.getUserInfo().split(":")(0)
        val password = dbUri.getUserInfo().split(":")(1)
        val dbUrl =
          s"jdbc:postgresql://${dbUri.getHost()}:${dbUri.getPort()}${dbUri.getPath()}?sslmode=require"

        override def apply[A](k: Connection => A): Task[A] =
          val conn =
            DriverManager.getConnection(dbUrl, username, password)
          ZIO
            .attempt {
              conn.setAutoCommit(false)
              val result: A = k(conn)
              conn.commit()
              result
            }
            .catchAll { e =>
              LoggerFactory
                .getLogger(this.getClass())
                .error("rolling back transaction")
              conn.rollback()
              ZIO.fail(e)
            }
    }

  def apply[A](k: Connection => A): RIO[Db, A] =
    ZIO.accessZIO(_.get.apply(k))
