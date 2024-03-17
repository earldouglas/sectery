let

  nixpkgs-src =
    builtins.fetchTarball {
      url = "https://github.com/NixOS/nixpkgs/archive/5863c27.tar.gz";
      sha256 = "0hcqikq9hm7kqklgr9w7djw7m8y5yvq1yajps2v0v04mgg97ykwj";
    };

  nixpkgs =
    (import nixpkgs-src) {
      overlays = [ sbt-derivation sbt-overlay ];
    };

  sbt-derivation-src =
    builtins.fetchTarball {
      url = "https://github.com/zaninime/sbt-derivation/archive/6762cf2.tar.gz";
      sha256 = "0g9dzw734k4qhvc4h88zjbrxdiz6g8kgq7qgbac8jgj8cvns6xry";
    };

  sbt-derivation = import "${sbt-derivation-src}/overlay.nix";

  sbt-overlay =
    final: prev: {
      sbt = prev.sbt.override {
        jre = prev.jdk17;
      };
    };

  buildCmd = "sbt scalafmtCheckAll scalafmtSbtCheck test assembly";

in

  nixpkgs.mkSbtDerivation {

    pname = "sectery";
    version = "1.0.0";

    depsSha256 = "sha256-aWzpZTlLpNJ8nSF6dCG3Go4wu/j4IysbIhQtJaxJmhM=";

    src = ./.;

    depsWarmupCommand = buildCmd;

    buildPhase = buildCmd;

    installPhase = ''
      mkdir -p $out/
      cp modules/irc/target/scala-*/irc.jar $out/
      cp modules/producers/target/scala-*/producers.jar $out/
    '';
  }
