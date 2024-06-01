package sectery.usecases

import sectery.control.Monad._
import sectery.control._
import sectery.domain.entities._
import sectery.effects._
import sectery.usecases.responders._

class Responders[
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
    : Krypto //
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
]:

  val responders: List[Responder[F]] =
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
        new KryptoResponder,
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

  def respondToMessage(rx: Rx): F[List[Tx]] =
    summon[Traversable[F]]
      .traverse(responders)(_.respondToMessage(rx))
      .map(_.flatten)
