#!/bin/bash

set -e

source ./.ci/util.sh

checkForVariable "GPG_SECRET_KEY"

mkdir -p ~/.gnupg/
echo "$GPG_SECRET_KEY" > ~/.gnupg/private.key
gpg --batch --import ~/.gnupg/private.key
chmod 600 ~/.gnupg/private.key
chmod 700 ~/.gnupg
