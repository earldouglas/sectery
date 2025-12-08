#!/usr/bin/env bash

set -euxo pipefail

nixos-rebuild switch \
  -I nixpkgs=https://github.com/NixOS/nixpkgs/archive/7fb759a.tar.gz \
  --fast \
  --build-host root@$HOST_NAME.$DOMAIN \
  --target-host root@$HOST_NAME.$DOMAIN \
  -I nixos-config=./infra/configuration.nix
