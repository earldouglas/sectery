package sectery

import java.sql._
import zio._

object Db:

  type Db = Has[Db.Service]

  trait Service:
    def query[A](k: Connection => A): Task[A]

  val live: ULayer[Has[Service]] =
    ZLayer.succeed {
      new Service:
        Class.forName("org.sqlite.JDBC");
        lazy val dbPath = sys.env("SECTERY_DB")
        lazy val conn: Connection =
          DriverManager.getConnection("""jdbc:sqlite:${dbPath}""")
        override def query[A](k: Connection => A): Task[A] =
          ZIO.effect(k(conn))
    }

  def query[A](k: Connection => A): RIO[Db, A] =
    ZIO.accessM(_.get.query(k))
