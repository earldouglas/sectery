{
  pkgs,
  slf4jSimple,
  mavenRepo,
}:

let

  slackJar = pkgs.fetchurl {
    url = "${mavenRepo}/sectery/slack-assembly_3/0.1.0-SNAPSHOT/slack-assembly_3-0.1.0-SNAPSHOT.jar";
    sha256 = "";
  };

in
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
      ExecStart = "${pkgs.jdk25}/bin/java -server -Xms192m -Xmx192m -cp ${slf4jSimple} -jar ${slackJar}";
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
