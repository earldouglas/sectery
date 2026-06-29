let

  nixpkgs = builtins.fetchTarball {
    # nix-prefetch-git --branch-name nixos-26.05 https://github.com/NixOS/nixpkgs.git
    url = "https://github.com/NixOS/nixpkgs/archive/de0d0d5.tar.gz";
    sha256 = "0hzj56kp42h7fiac6d4av051s71rqcc8c6wx2d8qqhh5x3wgrqca";
  };

  pkgs = import nixpkgs { };

  version = "0.1.0-SNAPSHOT";

  sbt = import ./sbt.nix {
    inherit pkgs;
    jdk = pkgs.jdk21;
    depsWarmupCommand = ''
      sbt \
        update \
        scalafmtCheckAll \
        scalafmtSbtCheck \
        "scalafixAll --check"
    '';
    depsSha256 = "sha256-+8fdjLisFRceIC9k02C/8X5ZizJcA09NaiX6PqoZ5Rw=";
  };

in

pkgs.stdenv.mkDerivation {

  inherit version;

  pname = "sectery";

  buildInputs = [
    sbt
  ];

  src = ./.;

  buildPhase = ''
    sbt test assembly
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
