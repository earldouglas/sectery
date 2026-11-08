{
  pkgs,
  mavenHost,
  mavenUsername,
  mavenPassword,
  mavenRepo,
}:

let

  sectery-irc = pkgs.stdenv.mkDerivation rec {

    pname = "sectery-irc";
    version = "0.1.0-SNAPSHOT";

    buildInputs = [
      pkgs.coursier
    ];

    outputHashAlgo = "sha256";
    outputHashMode = "recursive";
    outputHash = "sha256-iLLAdUQidhVYF888J7j1SN5T4PcQjRrAdtl/PwL/NW0=";

    src = null;
    dontUnpack = true;

    installPhase = ''

      mkdir -p $out/cache

      cs fetch \
        --quiet \
        --credentials "${mavenHost} ${mavenUsername}:${mavenPassword}" \
        --repository "${mavenRepo}" \
        --cache $out/cache \
        com.earldouglas:sectery-irc_3:${version}

      find $out/cache \
        -name "sectery-irc_3-*.jar" \
          -exec cp {} $out/sectery-irc.jar \;

      rm -rf $out/cache
    '';
  };

in
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
      ExecStart = "${pkgs.jdk25}/bin/java -server -Xms192m -Xmx192m -jar ${sectery-irc}/sectery-irc.jar";
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
