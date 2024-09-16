#!/usr/bin/env bash

ROOT="$(dirname "$(dirname "$(readlink -fm "$0")")")"

SUMMARY=$(nix-prefetch-git --quiet "$ROOT")

REV=$(echo $SUMMARY | jq -r .rev)
SHA=$(echo $SUMMARY | jq -r .sha256)

sed -i "s/rev = \".*\"; # REV/rev = \"$REV\"; # REV/" "$ROOT/infra/configuration.nix"
sed -i "s/sha256 = \".*\"; # SHA/sha256 = \"$SHA\"; # SHA/" "$ROOT/infra/configuration.nix"
