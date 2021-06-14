package sectery

import scala.io.Source

object Resource:
  def read(path: String): String =
    Source
      .fromInputStream(getClass().getResourceAsStream(path))
      .mkString
