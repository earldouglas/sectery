package sectery.effects

trait Krypto[F[_]]:
  def getOrStartGame(service: String, channel: String): F[Krypto.Game]
  def deleteGame(service: String, channel: String): F[Unit]
  def setGuessCount(
      service: String,
      channel: String,
      guessCount: Int
  ): F[Unit]

object Krypto:
  case class Game(
      guessCount: Int,
      objective: Int,
      cards: (Int, Int, Int, Int, Int)
  )

  val deck: List[Int] =
    (1 to 6).toList.flatMap(i => List(i, i, i)) ++
      (7 to 10).toList.flatMap(i => List(i, i, i, i)) ++
      (11 to 17).toList.flatMap(i => List(i, i)) ++
      (18 to 25).toList

  def newGame: Krypto.Game =
    val shuffled: List[Int] = deck.sortBy(_ => Math.random())
    Krypto.Game(
      guessCount = 0,
      objective = shuffled(0),
      cards = (
        shuffled(1),
        shuffled(2),
        shuffled(3),
        shuffled(4),
        shuffled(5)
      )
    )
