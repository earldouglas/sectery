let

  mkSbt =
    {
      pkgs,
      jdk,
    }:
    let
      sbt-version = "2.0.0-RC14";
      sbt-launch-jar = pkgs.fetchurl {
        url = "https://repo1.maven.org/maven2/org/scala-sbt/sbt-launch/${sbt-version}/sbt-launch-${sbt-version}.jar";
        hash = "sha256-z3/zEyxrLm6fNabwRAqVZDTYTshIhjThBLXZ83tqZgI=";
      };
    in
    pkgs.writeScriptBin "sbt" ''
      mkdir -p target/.sbtboot
      mkdir -p target/.coursier
      mkdir -p target/.boot
      mkdir -p target/.ivy
      export COURSIER_CACHE=target/.coursier
      ${jdk}/bin/java \
        -Dsbt.global.base=target/.sbtboot \
        -Dsbt.boot.directory=target/.boot \
        -Dsbt.ivy.home=target/.ivy \
        -Dsbt.io.virtual=false \
        -Dsbt.server.autostart=false \
        -jar ${sbt-launch-jar} \
        "$@"
    '';

  mkDeps =
    {
      pkgs,
      sbt,
      buildPhase,
      outputHash,
    }:
    pkgs.stdenv.mkDerivation {

      pname = "sbt-deps";
      version = "0.1.0-SNAPSHOT";

      buildInputs = [
        sbt
      ];

      outputHashAlgo = "sha256";
      outputHashMode = "recursive";
      outputHash = outputHash;

      src = ./.;

      buildPhase = buildPhase;

      installPhase = ''
        mkdir -p $out
        cp -r target/.coursier $out/
        cp -r target/.boot $out/
        cp -r target/.ivy $out/
      '';

    };

in

{
  pkgs,
  jdk,
  depsWarmupCommand,
  depsSha256,
}:

let

  sbt = mkSbt {
    inherit pkgs;
    jdk = jdk;
  };

  deps = mkDeps {
    inherit pkgs;
    sbt = sbt;
    buildPhase = depsWarmupCommand;
    outputHash = depsSha256;
  };

in

pkgs.writeShellScriptBin "sbt" ''
  ${pkgs.rsync}/bin/rsync -a ${deps}/ target/
  chmod -R u+w target/
  ${sbt}/bin/sbt "$@"
''
