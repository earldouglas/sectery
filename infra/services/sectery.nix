{ pkgs }:

let

  src = pkgs.fetchFromGitHub {
    owner = "earldouglas";
    repo = "sectery";
    rev = "06eb0a6190ef0f8cf11846ff18868b6e00691393";
    hash = "sha256-tio2TAJ02uwiA5+Tdlp8x7gLMYGo337166ENRyibnU0=";
  };

  sbt = import ./sbt.nix {
    inherit pkgs src;
    jdk = pkgs.jdk25;
    depsWarmupCommand = ''
      sbt \
        update
    '';
    depsSha256 = "sha256-ZTccn5uBY+AC/hXXDBWw7nTQS2JinWz8fLWzlLFTeEA=";
  };

in

pkgs.stdenv.mkDerivation {

  inherit src;

  version = "0.1.0-SNAPSHOT";

  pname = "sectery";

  buildInputs = [
    sbt
  ];

  buildPhase = ''
    sbt \
      test \
      assembly
  '';

  installPhase = ''
    mkdir -p $out/
    cp target/out/jvm/scala-*/irc/irc.jar $out/
    cp target/out/jvm/scala-*/producers/producers.jar $out/
    cp target/out/jvm/scala-*/slack/slack.jar $out/
  '';

  meta = {
    description = "A digital assistant chatbot";
    homepage = "https://github.com/earldouglas/sectery";
    license = pkgs.lib.licenses.mit;
    maintainers = [ pkgs.lib.maintainers.earldouglas ];
  };

}
