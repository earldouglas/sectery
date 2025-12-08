{
  pkgs,
  sectery,
  slf4jSimple,
}:
{

  # Service account ####################################################
  users.groups.slack = { };
  users.users.slack = {
    group = "slack";
    isSystemUser = true;
  };

  # Service ############################################################
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

}
