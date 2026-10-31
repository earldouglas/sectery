{
  pkgs ? import <nixpkgs> { },
}:

let

  jdk = pkgs.jdk25;

  derivations = builtins.fetchGit {
    url = "https://gitlab.com/earldouglas/derivations.git";
    rev = "44146c8bb7dec004f2cb2adc2da392c7d7182fd5";
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
        scalafmtSbtCheck \
        scalafmtCheckAll \
        integration/scalafmtCheckAll \
        "scalafixAll --check" \
        "integration/scalafixAll --check" \
        "integration/Test/compile" \
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
