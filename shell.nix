{
  pkgs ? import <nixpkgs> { },
}:

let

  jdk = pkgs.jdk17;

  derivations = builtins.fetchGit {
    url = "https://git.earldouglas.com/earldouglas/derivations.git";
    rev = "dea734ec0060884de222afd122180817e123f7b5";
  };

  metals = (import "${derivations}/metals/default.nix") {
    inherit pkgs jdk;
  };

  nvim = (import "${derivations}/nvim/scala/default.nix") {
    inherit pkgs metals;
  };

  sbt = (import "${derivations}/sbt/default.nix") {
    inherit pkgs jdk;
  };

in

pkgs.mkShell {

  nativeBuildInputs = [ sbt ] ++ (if builtins.getEnv "CI" == "true" then [ ] else [ nvim ]);

  shellHook = ''
    sbt-test() {
      sbt \
        scalafmtCheckAll \
        "scalafixAll --check" \
        test
    }

    sbt-fmt() {
      sbt \
        scalafixAll \
        scalafmtAll \
        scalafmtSbt
    }
  '';
}
