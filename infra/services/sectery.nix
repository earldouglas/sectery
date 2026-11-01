{ pkgs }:

let

  src = pkgs.fetchFromGitHub {
    owner = "earldouglas";
    repo = "sectery";
    rev = "da0b79b39d9048eceb4a3c206fc3d03de24d1521";
    hash = "sha256-hwWMWC2B6l6dewr09GGXSdXyTsv6Nf9mE/NbBu7aMeU=";
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
