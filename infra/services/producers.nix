{
  pkgs,
  mavenHost,
  mavenUsername,
  mavenPassword,
  mavenRepo,
}:

let

  sectery-producers = pkgs.stdenv.mkDerivation rec {

    pname = "sectery-producers";
    version = "0.1.0-SNAPSHOT";

    buildInputs = [
      pkgs.coursier
    ];

    outputHashAlgo = "sha256";
    outputHashMode = "recursive";
    outputHash = "sha256-RIe9AjsR79n/Mlcw/N5jlAXrCOXvD6wKlWgkiCSdxGY=";

    src = null;
    dontUnpack = true;

    installPhase = ''

      mkdir -p $out/cache

      cs fetch \
        --quiet \
        --credentials "${mavenHost} ${mavenUsername}:${mavenPassword}" \
        --repository "${mavenRepo}" \
        --cache $out/cache \
        com.earldouglas:sectery-producers_3:${version}

      find $out/cache \
        -name "sectery-producers_3-*.jar" \
          -exec cp {} $out/sectery-producers.jar \;

      rm -rf $out/cache
    '';
  };

in
{

  # Service account ####################################################
  users.groups.producers = { };
  users.users.producers = {
    group = "producers";
    isSystemUser = true;
    home = "/home/producers";
    createHome = true;
  };

  # Database ###########################################################
  services.mysql = {
    enable = false;
    package = pkgs.mariadb_118;
    settings = {
      mariadb = {
        plugin_load_add = "auth_mysql_sha2";
      };
    };
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
      ExecStart = "${pkgs.jdk25}/bin/java -server -Xss192m -Xmx192m -jar ${sectery-producers}/sectery-producers.jar";
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
      GEODE_URL = builtins.getEnv "GEODE_URL";
      LLM_URL = builtins.getEnv "LLM_URL";
    };
  };

}
