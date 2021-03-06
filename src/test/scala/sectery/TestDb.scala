package sectery

import java.sql.Connection
import java.sql.DriverManager
import zio.Has
import zio.Task
import zio.ULayer
import zio.ZIO
import zio.ZLayer

object TestDb:
  def apply(): ULayer[Has[Db.Service]] =
    ZLayer.succeed {
      new Db.Service:
        Class.forName("org.sqlite.JDBC");
        lazy val conn: Connection =
          DriverManager.getConnection(s"jdbc:sqlite::memory:")
        override def query[A](k: Connection => A): Task[A] =
          ZIO.effect(k(conn))
    }
