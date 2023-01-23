let
  sbt-derivation = import (builtins.fetchTarball {
    url =
      "https://github.com/zaninime/sbt-derivation/archive/1ef212261cf7ad878c253192a1c8171de4d75b6d.tar.gz";
    sha256 = "1mz2s4hajc9cnrfs26d99ap4gswcidxcq441hg3aplnrmzrxbqbp";
  });
in { pkgs ? import <nixpkgs> { overlays = [ sbt-derivation ]; } }:
pkgs.sbt.mkDerivation {

  pname = "sectery";
  version = "1.0.0";

  depsSha256 = "sha256-u1mkXkykpNmmTmANctGD/G3plkh29ybzEvI6H5OlaWg=";

  src = ./.;

  buildPhase = ''
    sbt assembly
  '';

  installPhase = ''
    mkdir -p $out/
    cp modules/irc/target/scala-*/irc.jar $out/
    cp modules/producers/target/scala-*/producers.jar $out/
  '';
}
