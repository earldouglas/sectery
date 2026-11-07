{
  config,
  lib,
  pkgs,
  modulesPath,
  ...
}:

let

  getEnv =
    name:
    let
      value = builtins.getEnv name;
    in
    if builtins.stringLength value == 0 then throw "${name} env var is required" else value;

  hostName = getEnv "HOST_NAME";
  domain = getEnv "DOMAIN";
  mavenRepo = getEnv "MAVEN_REPO";

  slf4jSimple = pkgs.fetchurl {
    url = "https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.17/slf4j-simple-2.0.17.jar";
    sha256 = "sha256-3f6lmsB0xtPiSsLDhiLS2WOJXhf3CzjtS9rk14C+aWQ=";
  };

  system = import ./services/system.nix {
    inherit modulesPath;
    inherit hostName domain;
  };

  rabbitmq = import ./services/rabbitmq.nix {
  };

  producers = import ./services/producers.nix {
    inherit pkgs mavenRepo slf4jSimple;
  };

  irc = import ./services/irc.nix {
    inherit pkgs mavenRepo slf4jSimple;
  };

in
{

  imports = [
    system
    rabbitmq
    producers
    irc
  ];
}
