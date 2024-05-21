package sectery

import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import scala.io.Source

object Resource:

  val decoder =
    val d = Charset.forName("UTF-8").newDecoder()
    d.onMalformedInput(CodingErrorAction.IGNORE)
    d

  def read(path: String): String =
    Source
      .fromInputStream(getClass().getResourceAsStream(path))(decoder)
      .mkString
