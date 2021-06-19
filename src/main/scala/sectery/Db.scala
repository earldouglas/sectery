package sectery

import java.net.URI
import java.sql._
import zio._

object Db:

  type Db = Has[Db.Service]

  trait Service:
    def query[A](k: Connection => A): Task[A]

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

        val conn: Connection =
          DriverManager.getConnection(dbUrl, username, password)

        override def query[A](k: Connection => A): Task[A] =
          ZIO.effect(k(conn))
    }

  def query[A](k: Connection => A): RIO[Db, A] =
    ZIO.accessM(_.get.query(k))
