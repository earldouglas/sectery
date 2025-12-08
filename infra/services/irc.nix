{
  pkgs,
  sectery,
  slf4jSimple,
}:
{

  # Service account ####################################################
  users.groups.irc = { };
  users.users.irc = {
    group = "irc";
    isSystemUser = true;
  };

  # Service ############################################################
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

}
