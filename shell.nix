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

  SBT_OPTS = "--enable-native-access=ALL-UNNAMED";

  shellHook = ''
    sectery-test() {
      sbt \
        scalafmtSbtCheck \
        scalafmtCheckAll \
        integration/scalafmtCheckAll \
        "scalafixAll --check" \
        "integration/scalafixAll --check" \
        "integration/Test/compile" \
        test
    }

    sectery-fmt() {
      sbt \
        scalafixAll \
        scalafmtAll \
        scalafmtSbt
    }


    sectery-publish() {
      sbt '
        set ThisBuild / organization := "com.earldouglas";
        set ThisBuild / publishTo := Some("Maven Repo" at sys.env("MAVEN_REPO"));
        set ThisBuild / credentials += Credentials("Maven Repo", sys.env("MAVEN_HOST"), sys.env("MAVEN_USERNAME"), sys.env("MAVEN_PASSWORD"));
        set ThisBuild / versionScheme := Some("semver-spec");
        publish
      '
    }

    sectery-deploy() {
      nixos-rebuild switch \
        --fast \
        --target-host root@$HOST_NAME.$DOMAIN \
        -I nixos-config=./infra/configuration.nix
    }
  '';

}
