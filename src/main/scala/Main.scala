import java.io.IOException
import zio._
import zio.console._

object HelloWorld {
  def sayHello: ZIO[Console, IOException, Unit] =
    console.putStrLn("Hello, World!")
}
