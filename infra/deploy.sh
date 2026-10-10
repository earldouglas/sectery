#!/usr/bin/env bash

set -euxo pipefail

nix-shell ./shell.nix --run "sbt integration/test"

nixos-rebuild switch \
  --fast \
  --build-host root@$HOST_NAME.$DOMAIN \
  --target-host root@$HOST_NAME.$DOMAIN \
  -I nixos-config=./infra/configuration.nix
