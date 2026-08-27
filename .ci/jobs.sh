#!/bin/bash
set -e

source ./.ci/util.sh

case $1 in

verify)
  ./mvnw -B clean verify
  ;;

release-dry-run)
  ./mvnw -e -ntp -B \
    release:prepare \
    -DdryRun=true \
    -Darguments='-Pcentral-release -Dgpg.skip=true -ntp'
  ./mvnw -B release:clean
  ;;

clean-install-skip-tests)
  ./mvnw -B clean install -DskipTests
  ;;

*)
  echo "Unexpected argument: $1"
  sleep 5s
  exit 1
  ;;

esac
