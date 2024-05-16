package sectery.usecases

import sectery.control.Monad._
import sectery.control._
import sectery.domain.entities._
import sectery.domain.operations._
import sectery.effects._
import sectery.usecases.responders._

class RespondToMessage[
    F[_] //
    : Btc //
    : Counter //
    : Eval //
    : Frinkiac //
    : GetConfig //
    : GetWx //
    : Grab //
    : Hack //
    : HttpClient //
    : LastMessage //
    : Monad //
    : Now //
    : OpenAI //
    : Points //
    : Quote //
    : SetConfig //
    : Stock //
    : Tell //
    : Traversable //
    // : ReceiveMessage //
    : SendMessage //
] extends ReceiveMessage[F]:

  private val responders: List[Responder[F]] =
    val allExceptHelp: List[Responder[F]] =
      List(
        new AsciiResponder,
        new BlinkResponder,
        new BtcResponder,
        new CountResponder,
        new EvalResponder,
        new FrinkiacResponder,
        new GetResponder,
        new GrabResponder,
        new HackResponder,
        new HtmlResponder,
        new MorseResponder,
        new OpenAIResponder,
        new PingResponder,
        new PointsResponder,
        new QuoteResponder,
        new SetResponder,
        new StockResponder,
        new SubstituteResponder,
        new TellResponder,
        new TimeResponder,
        new VersionResponder,
        new WxResponder
      )
    new HelpResponder(allExceptHelp) :: allExceptHelp

    /*
  def apply(): F[Unit] =
    summon[ReceiveMessage[F]]
      .receiveMessage()
      .flatMap { rx =>
        summon[Traversable[F]]
          .traverse(responders)(_.respondToMessage(rx))
          .map(_.flatten)
      }
      .flatMap { txs =>
        summon[Traversable[F]]
          .traverse(txs) { tx =>
            summon[SendMessage[F]]
              .sendMessage(tx)
          }
      }
      .map { xs =>
        xs.fold(())((_, _) => ())
      }
     */

  override def receiveMessage(rx: Rx): F[Unit] =
    summon[Traversable[F]]
      .traverse(responders)(_.respondToMessage(rx))
      .map(_.flatten)
      .flatMap { txs =>
        summon[Traversable[F]]
          .traverse(txs) { tx =>
            summon[SendMessage[F]]
              .sendMessage(tx)
          }
      }
      .map { xs =>
        xs.fold(())((_, _) => ())
      }
