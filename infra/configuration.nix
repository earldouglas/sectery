let

  region = "us-east-2";
  accessKeyId = "sectery";

in {

  network.description = "Sectery";

  resources.ec2KeyPairs.sectery-key-pair = {
      inherit region accessKeyId;
    };

  sectery =
    { resources, pkgs, ... }:
    let

      sectery =
        import (pkgs.fetchFromGitHub {
          owner = "earldouglas";
          repo = "sectery";
          rev = "e789a45617af912ff62b12d0d96b99dc548972bc";
          sha256 = "0qgljmk5mzp0fn6c3skl21kn89ip4gpzkn6hby95w202mzrjrpa8";
        });

      slf4jSimple =
        pkgs.fetchurl {
          url = "https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.36/slf4j-simple-1.7.36.jar";
          sha256 = "sha256-Lzm+2UPWJN+o9BAtBXEoOhCHC2qjbxl6ilBvFHAQwQ8=";
        };

    in {

      nixpkgs.system = "aarch64-linux";

      # EC2 ############################################################
      deployment = {
        targetEnv = "ec2";
        ec2 = {
          accessKeyId = accessKeyId;
          region = region;
          instanceType = "t4g.micro";
          keyPair = resources.ec2KeyPairs.sectery-key-pair;
          ami = "ami-033ff64078c59f378";
          ebsInitialRootDiskSize = 12;
        };
      };

      # GC #############################################################
      nix.gc.automatic = true;
      nix.gc.options = "-d";
      nix.optimise.automatic = true;

      # Disable docs ###################################################
      documentation.enable = false;
      documentation.dev.enable = false;
      documentation.doc.enable = false;
      documentation.info.enable = false;
      documentation.man.enable = false;
      documentation.nixos.enable = false;

      # Security #######################################################
      services.fail2ban.enable = true;
      networking.firewall.allowedTCPPorts = [ 22 ];

      # RabbitMQ #######################################################
      services.rabbitmq.enable = true;

      # IRC Module #####################################################

      ## IRC Service User ##############################################
      users.groups.irc = {};
      users.users.irc = {
        group = "irc";
        isSystemUser = true;
      };

      ## IRC Service ###################################################
      systemd.services.irc = {
        description = "irc";
        after = [ "network.target" ];
        wantedBy = [ "multi-user.target" ];
        serviceConfig = {
          ExecStart = "${pkgs.jdk17_headless}/bin/java -server -Xms192m -Xmx192m -cp ${slf4jSimple}:${sectery}/irc.jar sectery.irc.Main";
          Restart = "always";
          User = "irc";
        };
        environment = {
          IRC_USER = builtins.getEnv "IRC_USER";
          IRC_PASS = builtins.getEnv "IRC_PASS";
          IRC_HOST = builtins.getEnv "IRC_HOST";
          IRC_PORT = builtins.getEnv "IRC_PORT";
          IRC_CHANNELS = builtins.getEnv "IRC_CHANNELS";
          RABBIT_MQ_HOSTNAME = builtins.getEnv "RABBIT_MQ_HOSTNAME";
          RABBIT_MQ_PORT = builtins.getEnv "RABBIT_MQ_PORT";
          RABBIT_MQ_USERNAME = builtins.getEnv "RABBIT_MQ_USERNAME";
          RABBIT_MQ_PASSWORD = builtins.getEnv "RABBIT_MQ_PASSWORD";
        };
      };
      services.cron = {
        enable = true;
        systemCronJobs = [
          "0 0 * * * systemctl restart irc" # https://xkcd.com/1495/
        ];
      };

      # Producers Module ###############################################

      ## Producers Service User ########################################
      users.groups.producers = {};
      users.users.producers = {
        group = "producers";
        isSystemUser = true;
      };

      ## Producers Database ############################################
      services.mysql = {
        enable = true;
        package = pkgs.mariadb;
        ensureDatabases = [
          "producers"
        ];
        ensureUsers = [
          {
            name = "producers";
            ensurePermissions = {
              "producers.*" = "ALL PRIVILEGES";
            };
          }
        ];
      };

      ## Producers Service #############################################
      systemd.services.producers = {
        description = "producers";
        after = [ "network.target" ];
        wantedBy = [ "multi-user.target" ];
        serviceConfig = {
          ExecStart = "${pkgs.jdk17}/bin/java -server -Xss192m -Xmx192m -cp ${slf4jSimple}:${sectery}/producers.jar sectery.producers.Main";
          Restart = "always";
          User = "producers";
        };
        environment = {
          DATABASE_URL = builtins.getEnv "DATABASE_URL";
          FINNHUB_API_TOKEN = builtins.getEnv "FINNHUB_API_TOKEN";
          OPEN_WEATHER_MAP_API_KEY = builtins.getEnv "OPEN_WEATHER_MAP_API_KEY";
          AIRNOW_API_KEY = builtins.getEnv "AIRNOW_API_KEY";
          RABBIT_MQ_HOSTNAME = builtins.getEnv "RABBIT_MQ_HOSTNAME";
          RABBIT_MQ_PORT = builtins.getEnv "RABBIT_MQ_PORT";
          RABBIT_MQ_USERNAME = builtins.getEnv "RABBIT_MQ_USERNAME";
          RABBIT_MQ_PASSWORD = builtins.getEnv "RABBIT_MQ_PASSWORD";
          OPENAI_APIKEY = builtins.getEnv "OPENAI_APIKEY";
        };
      };
    };
}
