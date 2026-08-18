#!/bin/bash

set -e

echo "Deploying jars to Maven Central ..."
./mvnw -B clean deploy -Pcentral-release -Pgpg
