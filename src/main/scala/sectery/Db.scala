package sectery

import java.net.URI
import java.sql._
import zio._

object Db:

  type Db = Has[Db.Service]

  trait Service:
    def query[A](k: Connection => A): Task[A]

  val live: ULayer[Has[Service]] =
    ZLayer.succeed {
      new Service:
        lazy val dbUri = new URI(sys.env("DATABASE_URL"))
        lazy val username = dbUri.getUserInfo().split(":")(0)
        lazy val password = dbUri.getUserInfo().split(":")(1)
        lazy val dbUrl =
          s"jdbc:postgresql://${dbUri.getHost()}:${dbUri.getPort()}${dbUri.getPath()}?sslmode=require"

        lazy val conn: Connection =
          DriverManager.getConnection(dbUrl, username, password)

        override def query[A](k: Connection => A): Task[A] =
          ZIO.effect(k(conn))
    }

  def query[A](k: Connection => A): RIO[Db, A] =
    ZIO.accessM(_.get.query(k))
