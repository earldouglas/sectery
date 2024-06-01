package sectery

trait HasService[A]:
  def getService(value: A): String
