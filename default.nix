let
  repository = builtins.fetchTarball {
    url = "https://github.com/zaninime/sbt-derivation/archive/92d6d6d.tar.gz";
    sha256 = "0hlpq1qzzvmswal3x02sv8hkl53bs9zrb62smwj3gnjm5a2qbi7s";
  };

  sbt-derivation = import "${repository}/overlay.nix";

  sbt-overlay =
    final: prev: {
      sbt = prev.sbt.override {
        jre = prev.jdk17;
      };
    };

in {
  pkgs ? import <nixpkgs> {
    overlays = [ sbt-derivation sbt-overlay ];
  }
}:
pkgs.mkSbtDerivation {

  pname = "sectery";
  version = "1.0.0";

  depsSha256 = "sha256-NMTSYNzgeDbMB+gaZq1Csi0W7dDG/0plAxwv7SbVfpA=";

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
