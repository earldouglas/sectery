{ pkgs }:

let

  _src = pkgs.fetchFromGitHub {
    owner = "earldouglas";
    repo = "sectery";
    rev = "df780888afa99c3b1954c0991cb4e5f81292e37c";
    hash = "";
  };

  src = builtins.fetchGit {
    url = /home/james/src/sectery;
  };

  sbt = import ./sbt.nix {
    inherit pkgs src;
    jdk = pkgs.jdk25;
    depsWarmupCommand = ''
      sbt \
        update
    '';
    depsSha256 = "";
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
