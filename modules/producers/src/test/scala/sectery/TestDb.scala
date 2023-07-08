package sectery

import java.sql.Connection
import java.sql.DriverManager
import zio.ULayer
import zio.ZIO
import zio.ZLayer

object TestDb:
  def apply(): ULayer[Db.Service] =
    val db: Db.Service =
      new Db.Service:
        Class.forName("org.h2.Driver");
        val conn: Connection =
          DriverManager.getConnection(s"jdbc:h2:mem:;MODE=MYSQL")
        override def apply[A](
            k: Connection => A
        ): ZIO[Any, Throwable, A] =
          ZIO.attempt(k(conn))
    ZLayer.succeed(db)
