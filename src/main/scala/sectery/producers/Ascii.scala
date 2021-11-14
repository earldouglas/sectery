package sectery.producers

import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.IOException
import sectery.Db
import sectery.Finnhub
import sectery.Http
import sectery.Info
import sectery.Producer
import sectery.Response
import sectery.Rx
import sectery.Tx
import zio.Clock
import zio.Has
import zio.RIO
import zio.Task
import zio.UIO
import zio.ZIO

object Ascii extends Producer:

  private val width = 80
  private val height = 10

  private val blink = """^@ascii\s+blink\s+(.+)$""".r
  private val ascii = """^@ascii\s+(.+)$""".r

  override def help(): Iterable[Info] =
    Some(Info("@ascii", "@ascii [blink] <text>"))

  override def apply(m: Rx): RIO[Db.Db with Http.Http, Iterable[Tx]] =
    m match
      case Rx(c, _, blink(text)) =>
        ZIO.succeed(
          ascii(text).map { line => Tx(c, s"\u0006${line}\u0006") }
        )
      case Rx(c, _, ascii(text)) =>
        ZIO.succeed(
          ascii(text).map { line => Tx(c, line) }
        )
      case _ =>
        ZIO.succeed(None)

  private def ascii(text: String): Seq[String] =
    val i = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g = i.getGraphics().asInstanceOf[Graphics2D]
    g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12))
    g.drawString(text, 0, height)
    ((0 until height) map { y =>
      (0 until width).map { x =>
        if i.getRGB(x, y) == -16777216 then " " else "#"
      }.mkString
    }).dropWhile(_.trim().length == 0)
