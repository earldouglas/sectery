{ pkgs }:

let

  src = pkgs.fetchFromGitHub {
    owner = "earldouglas";
    repo = "sectery";
    rev = "b6bb73493470ba3eb28270fae39683dea1e5c9cd";
    hash = "sha256-o/DY9SwoSNB6FwBSHLYEsbsij2ztu8xdN6pe3PKOtwM=";
  };

  sbt = import ./sbt.nix {
    inherit pkgs src;
    jdk = pkgs.jdk21;
    depsWarmupCommand = ''
      sbt \
        update \
        scalafmtCheckAll \
        scalafmtSbtCheck \
        "scalafixAll --check"
    '';
    depsSha256 = "sha256-/HHD2ijwJXiPMiWgF59xoIrYeUK3Pt7LImF5IQOr8Lk=";
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
