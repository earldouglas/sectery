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
          rev = "3a482012b016e6d51d211d9df13673e09b979865";
          sha256 = "0z490277m015pjxs186kjwh2n0xcb0vyn4hjfk9k3nrsgq5zniyq";
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
          RABBIT_MQ_HOSTNAME = builtins.getEnv "RABBIT_MQ_HOSTNAME";
          RABBIT_MQ_PORT = builtins.getEnv "RABBIT_MQ_PORT";
          RABBIT_MQ_USERNAME = builtins.getEnv "RABBIT_MQ_USERNAME";
          RABBIT_MQ_PASSWORD = builtins.getEnv "RABBIT_MQ_PASSWORD";
          IRC_USER = builtins.getEnv "IRC_USER";
          IRC_PASS = builtins.getEnv "IRC_PASS";
          IRC_HOST = builtins.getEnv "IRC_HOST";
          IRC_PORT = builtins.getEnv "IRC_PORT";
          IRC_CHANNELS = builtins.getEnv "IRC_CHANNELS";
        };
      };
      services.cron = {
        enable = true;
        systemCronJobs = [
          "0 0 * * * systemctl restart irc" # https://xkcd.com/1495/
        ];
      };

      # Slack Module ###################################################

      ## Slack Service User ############################################
      users.groups.slack = {};
      users.users.slack = {
        group = "slack";
        isSystemUser = true;
      };

      ## Slack Service #################################################
      systemd.services.slack = {
        description = "slack";
        after = [ "network.target" ];
        wantedBy = [ "multi-user.target" ];
        serviceConfig = {
          ExecStart = "${pkgs.jdk17_headless}/bin/java -server -Xms192m -Xmx192m -cp ${slf4jSimple}:${sectery}/slack.jar sectery.slack.Main";
          Restart = "always";
          User = "slack";
        };
        environment = {
          RABBIT_MQ_HOSTNAME = builtins.getEnv "RABBIT_MQ_HOSTNAME";
          RABBIT_MQ_PORT = builtins.getEnv "RABBIT_MQ_PORT";
          RABBIT_MQ_USERNAME = builtins.getEnv "RABBIT_MQ_USERNAME";
          RABBIT_MQ_PASSWORD = builtins.getEnv "RABBIT_MQ_PASSWORD";
          SLACK_BOT_TOKEN = builtins.getEnv "SLACK_BOT_TOKEN";
          SLACK_APP_TOKEN = builtins.getEnv "SLACK_APP_TOKEN";
        };
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
        initialDatabases = [
          {
            name = "producers";
            schema = ../producers.sql;
          }
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
          RABBIT_MQ_HOSTNAME = builtins.getEnv "RABBIT_MQ_HOSTNAME";
          RABBIT_MQ_PORT = builtins.getEnv "RABBIT_MQ_PORT";
          RABBIT_MQ_USERNAME = builtins.getEnv "RABBIT_MQ_USERNAME";
          RABBIT_MQ_PASSWORD = builtins.getEnv "RABBIT_MQ_PASSWORD";
          DATABASE_URL = builtins.getEnv "DATABASE_URL";
          FINNHUB_API_TOKEN = builtins.getEnv "FINNHUB_API_TOKEN";
          OPEN_WEATHER_MAP_API_KEY = builtins.getEnv "OPEN_WEATHER_MAP_API_KEY";
          AIRNOW_API_KEY = builtins.getEnv "AIRNOW_API_KEY";
          OPENAI_APIKEY = builtins.getEnv "OPENAI_APIKEY";
        };
      };
    };
}
