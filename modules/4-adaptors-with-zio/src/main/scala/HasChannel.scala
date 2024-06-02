package sectery

trait HasChannel[A]:
  def getChannel(value: A): String
