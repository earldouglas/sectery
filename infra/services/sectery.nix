{ pkgs }:

let

  src = pkgs.fetchFromGitHub {
    owner = "earldouglas";
    repo = "sectery";
    rev = "524079683687b4e8e26292ef4b15d35627623ccc";
    hash = "sha256-9zQ4xfyGWbDlXaao2cMC8EU67jicxAXWuNJtIJt5+UI=";
  };

  sbt = import ./sbt.nix {
    inherit pkgs src;
    jdk = pkgs.jdk25;
    depsWarmupCommand = ''
      sbt \
        update
    '';
    depsSha256 = "sha256-88n0XPmgE/Fq7xI3+rPdxZu2xTxOF8id4itI/hs4caQ=";
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
