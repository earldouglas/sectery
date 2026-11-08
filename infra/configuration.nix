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

  mavenHost = getEnv "MAVEN_HOST";
  mavenUsername = getEnv "MAVEN_USERNAME";
  mavenPassword = getEnv "MAVEN_PASSWORD";

  mavenRepo = getEnv "MAVEN_REPO";

  system = import ./services/system.nix {
    inherit modulesPath;
    inherit hostName domain;
  };

  rabbitmq = import ./services/rabbitmq.nix {
  };

  producers = import ./services/producers.nix {
    inherit
      pkgs
      mavenHost
      mavenUsername
      mavenPassword
      mavenRepo
      ;
  };

  irc = import ./services/irc.nix {
    inherit
      pkgs
      mavenHost
      mavenUsername
      mavenPassword
      mavenRepo
      ;
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
