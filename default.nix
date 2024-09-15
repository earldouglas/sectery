let

  nixpkgs-hash = "057f9ae"; # 23.11

  nixpkgs-src =
    builtins.fetchTarball {
      url = "https://github.com/NixOS/nixpkgs/archive/${nixpkgs-hash}.tar.gz";
      sha256 = "1ndiv385w1qyb3b18vw13991fzb9wg4cl21wglk89grsfsnra41k";
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

    depsSha256 = "sha256-EuKPEWrSJoP4ZeXsjewL8/hkhOLxu5Z5oxeESIduEn0=";

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
