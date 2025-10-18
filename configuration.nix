{
  resources,
  pkgs,
  lib,
  modulesPath,
  ...
}:

let

  sectery = import ./.;

  slf4jSimple = pkgs.fetchurl {
    url = "https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.17/slf4j-simple-2.0.17.jar";
    sha256 = "0r39ps0dgr6s9gnkh2zp2xg8jqyrs8i8dhy29bid7iklq2dabznx";
  };

in
{

  # Base Configuration ############################################

  ## hardware-configuration.nix ###################################
  imports = [ (modulesPath + "/profiles/qemu-guest.nix") ];
  boot.initrd.availableKernelModules = [
    "ahci"
    "xhci_pci"
    "virtio_pci"
    "virtio_scsi"
    "sd_mod"
    "sr_mod"
  ];
  boot.initrd.kernelModules = [ ];
  boot.kernelModules = [ ];
  boot.extraModulePackages = [ ];
  fileSystems."/" = {
    device = "/dev/disk/by-uuid/b9653418-a61b-498d-a806-5ead1842f90e";
    fsType = "ext4";
  };
  fileSystems."/boot" = {
    device = "/dev/disk/by-uuid/6B97-946A";
    fsType = "vfat";
    options = [
      "fmask=0022"
      "dmask=0022"
    ];
  };
  swapDevices = [ ];
  networking.useDHCP = lib.mkDefault true;
  nixpkgs.hostPlatform = lib.mkDefault "x86_64-linux";

  ## configuration.nix ############################################
  boot.loader.systemd-boot.enable = true;
  boot.loader.efi.canTouchEfiVariables = true;
  system.stateVersion = "24.11";

  # System ########################################################
  boot.loader.grub.device = "/dev/sda";
  networking.hostName = "bot";
  networking.domain = "sectery.com";
  services.openssh.enable = true;
  services.openssh.settings.PasswordAuthentication = false;
  time.timeZone = "America/New_York";
  environment.systemPackages = [
    pkgs.vim
    pkgs.git
  ];

  # Root user #####################################################
  users.users.root.openssh.authorizedKeys.keys = [
    "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIB/uBzLkbaPxQJpKxnToZ1uR4IKxq5h45WGCky7h5aFx root@sectery"
  ];

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
  users.groups.irc = { };
  users.users.irc = {
    group = "irc";
    isSystemUser = true;
  };

  ## IRC Service ###################################################
  systemd.services.irc = {
    enable = true;
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
  users.groups.slack = { };
  users.users.slack = {
    group = "slack";
    isSystemUser = true;
  };

  ## Slack Service #################################################
  systemd.services.slack = {
    enable = true;
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
  users.groups.producers = { };
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
    enable = true;
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
      DATABASE_DRIVER = builtins.getEnv "DATABASE_DRIVER";
      DATABASE_URL = builtins.getEnv "DATABASE_URL";
      FINNHUB_API_TOKEN = builtins.getEnv "FINNHUB_API_TOKEN";
      OPEN_WEATHER_MAP_API_KEY = builtins.getEnv "OPEN_WEATHER_MAP_API_KEY";
      AIRNOW_API_KEY = builtins.getEnv "AIRNOW_API_KEY";
      OPENAI_APIKEY = builtins.getEnv "OPENAI_APIKEY";
    };
  };

}
