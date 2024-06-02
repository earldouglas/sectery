package sectery.effects

trait Hack[F[_]]:
  def getOrStartGame(service: String, channel: String): F[(String, Int)]
  def isAWord(word: String): F[Boolean]
  def deleteGame(service: String, channel: String): F[Unit]
  def setGuessCount(
      service: String,
      channel: String,
      guessCount: Int
  ): F[Unit]
