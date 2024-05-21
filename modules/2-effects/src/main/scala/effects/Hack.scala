package sectery.effects

trait Hack[F[_]]:
  def getOrStartGame(channel: String): F[(String, Int)]
  def isAWord(word: String): F[Boolean]
  def deleteGame(channel: String): F[Unit]
  def setGuessCount(channel: String, guessCount: Int): F[Unit]
