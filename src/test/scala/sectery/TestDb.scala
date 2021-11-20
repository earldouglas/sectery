package sectery

import java.sql.Connection
import java.sql.DriverManager
import zio.Task
import zio.ULayer
import zio.ZIO
import zio.ZLayer

object TestDb:
  def apply(): ULayer[Db.Service] =
    ZLayer.succeed {
      new Db.Service:
        Class.forName("org.sqlite.JDBC");
        lazy val conn: Connection =
          DriverManager.getConnection(s"jdbc:sqlite::memory:")
        override def apply[A](k: Connection => A): Task[A] =
          ZIO.attempt(k(conn))
    }
