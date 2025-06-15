package sectery.adaptors

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.URI
import java.net.URL
import munit.FunSuite
import sectery._
import sectery.effects._
import sectery.effects.id._
import sectery.effects.id.given

class LiveHttpClientSuite extends FunSuite:

  val httpClient: HttpClient[Id] = LiveHttpClient()

  val httpServer = FunFixture[HttpServer](
    setup = { _ =>
      val httpServer: HttpServer =
        HttpServer.create(new InetSocketAddress(31337), 0)
      httpServer.createContext(
        "/hello",
        new HttpHandler:
          override def handle(t: HttpExchange): Unit = {
            val response: String = "Hello, world!"
            t.sendResponseHeaders(200, response.length())
            val os: OutputStream = t.getResponseBody()
            os.write(response.getBytes())
            os.close()
          }
      )
      httpServer.setExecutor(null)
      httpServer.start()
      httpServer
    },
    teardown = { httpServer =>
      httpServer.stop(0)
    }
  )

  httpServer.test("@eval 6 * 7") { _ =>

    val response =
      httpClient
        .request(
          method = "GET",
          headers = Map.empty,
          url = new URI("http://localhost:31337/hello").toURL(),
          body = None
        )

    assertEquals(
      obtained = response.status,
      expected = 200
    )

    assertEquals(
      obtained = response.body,
      expected = "Hello, world!"
    )
  }
