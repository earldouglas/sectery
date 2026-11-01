{ pkgs }:

let

  src = pkgs.fetchFromGitHub {
    owner = "earldouglas";
    repo = "sectery";
    rev = "ded67b708c0bb04507ff0397a951d55e5ad5ce10";
    hash = "sha256-ZwXgrhvE1Jmx7xGqB3oo96wU3g8FNQlAgprENkqcP4g=";
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
