package sectery.usecases.responders

import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import sectery.control.Monad
import sectery.domain.entities._
import sectery.usecases.Responder

class AsciiResponder[F[_]: Monad] extends Responder[F]:

  override def name = "@ascii"

  override def usage = "@ascii <text>"

  private val ascii = """^@ascii\s+(.+)$""".r

  override def respondToMessage(rx: Rx) =
    rx.message match
      case ascii(text) =>
        summon[Monad[F]].pure(
          AsciiResponder.ascii(text).map { line =>
            Tx(
              service = rx.service,
              channel = rx.channel,
              thread = rx.thread,
              message = line
            )
          }
        )
      case _ =>
        summon[Monad[F]].pure(Nil)

object AsciiResponder:

  private val width = 80
  private val height = 24
  private val base = 16

  def ascii(text: String): List[String] =
    val i = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g = i.getGraphics().asInstanceOf[Graphics2D]
    g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12))
    g.drawString(text, 0, base)
    (0 until height)
      .map { y =>
        (0 until width)
          .map { x =>
            if i.getRGB(x, y) == -16777216 then " " else "#"
          }
          .mkString
          .replaceAll("\\s*$", "")
      }
      .dropWhile(_.trim().length == 0)
      .reverse
      .dropWhile(_.trim().length == 0)
      .reverse
      .toList
