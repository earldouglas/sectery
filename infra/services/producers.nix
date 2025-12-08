{
  pkgs,
  sectery,
  slf4jSimple,
}:
{

  # Service account ####################################################
  users.groups.producers = { };
  users.users.producers = {
    group = "producers";
    isSystemUser = true;
  };

  # Database ###########################################################
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

  # Service ############################################################
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
