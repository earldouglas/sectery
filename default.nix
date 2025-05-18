let

  # https://github.com/NixOS/nixpkgs/commits/nixos-24.11/
  nixpkgs-rev = "e24b4c0";

  nixpkgs-src =
    builtins.fetchTarball {
      url = "https://github.com/NixOS/nixpkgs/archive/${nixpkgs-rev}.tar.gz";
      sha256 = "1m383nldy1jvl8n2jw02l26w9iib4b2a9341c1kf74miaahw7qx6";
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

    depsSha256 = "sha256-GXa3Hxx+/fWx5c14eoI0qL0nWNs5RxJhF3PcWKUnoPY=";

    src = ./.;

    depsWarmupCommand = buildCmd;

    buildPhase = buildCmd;

    installPhase = ''
      mkdir -p $out/
      cp modules/5-irc/target/scala-*/irc.jar $out/
      cp modules/5-slack/target/scala-*/slack.jar $out/
      cp modules/5-producers/target/scala-*/producers.jar $out/
    '';
  }
