{ pkgs }:

let

  src =
    pkgs.fetchFromGitHub {
      owner = "earldouglas";
      repo = "sectery";
      rev = "50bead10a396feea52f66967b4dc4698221241ee";
      hash = "sha256-GfvikPXecQbJBbfbYBWlekcA6BQo2CHXnAsYWg02e+A=";
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
    depsSha256 = "sha256-zkn3SaCifBcag6i7Lhp0zEwwPRL77NpFozrt53mtrqc=";
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
    sbt test assembly
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
