let

  pkgs =

    let

      pkgs = import <nixpkgs> { };

      sbt-derivation-src = pkgs.fetchFromGitHub {
        owner = "zaninime";
        repo = "sbt-derivation";
        rev = "6762cf2c31de50efd9ff905cbcc87239995a4ef9";
        sha256 = "sha256-Pnej7WZIPomYWg8f/CZ65sfW85IfIUjYhphMMg7/LT0=";
      };

      sbt-derivation = import "${sbt-derivation-src}/overlay.nix";

      sbt-overlay = final: prev: {
        sbt = prev.sbt.override {
          jre = prev.jdk21;
        };
      };

    in

    import <nixpkgs> {
      overlays = [
        sbt-derivation
        sbt-overlay
      ];
    };

in

pkgs.mkSbtDerivation {

  pname = "sectery";
  version = "0.1.0-SNAPSHOT";

  depsSha256 = "sha256-Ufabz8vj+BFfQLCa10VTBa0QKoN/x1FWX0O+iDibNM0=";

  src = ./.;

  depsWarmupCommand = ''
    sbt \
      scalafmtCheckAll \
      scalafmtSbtCheck \
      "scalafixAll --check" \
      test
  '';

  buildPhase = ''
    sbt \
      assembly
  '';


  installPhase = ''
    mkdir -p $out/
    cp modules/5-irc/target/scala-*/irc.jar $out/
    cp modules/5-slack/target/scala-*/slack.jar $out/
    cp modules/5-producers/target/scala-*/producers.jar $out/
  '';

  meta = {
    description = "A digital assistant chatbot";
    homepage = "https://github.com/earldouglas/sectery";
    license = pkgs.lib.licenses.mit;
    maintainers = [ pkgs.lib.maintainers.earldouglas ];
  };
}
