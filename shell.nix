let
  pkgs = import (fetchTarball http://nixos.org/channels/nixos-22.11/nixexprs.tar.xz) {};
  jdk = pkgs.jdk17;
in
  pkgs.mkShell {
    nativeBuildInputs = [
      (pkgs.sbt.override { jre = jdk; })
    ];
    shellHook = ''
      export JAVA_HOME=${jdk}
      PATH="${jdk}/bin:$PATH"
    '';
  }
