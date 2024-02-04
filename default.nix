let

  nixpkgs-src =
    builtins.fetchTarball {
      url = "https://github.com/NixOS/nixpkgs/archive/e92b601.tar.gz";
      sha256 = "0vq05559vmj5px94hr148wnq9qbn9whsfpwdbk8wn2myby4knwy7";
    };

  nixpkgs =
    (import nixpkgs-src) {
      overlays = [ sbt-derivation sbt-overlay ];
    };

  sbt-derivation-src =
    builtins.fetchTarball {
      url = "https://github.com/zaninime/sbt-derivation/archive/92d6d6d.tar.gz";
      sha256 = "0hlpq1qzzvmswal3x02sv8hkl53bs9zrb62smwj3gnjm5a2qbi7s";
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

    depsSha256 = "sha256-gg3nUjqeZ13nm/KqkIZ0hwLNVMR1muP36uxCuqTytZc=";

    src = ./.;

    depsWarmupCommand = buildCmd;

    buildPhase = buildCmd;

    installPhase = ''
      mkdir -p $out/
      cp modules/irc/target/scala-*/irc.jar $out/
      cp modules/producers/target/scala-*/producers.jar $out/
    '';
  }
