#!/bin/bash
set -euo pipefail

source ./.ci/util.sh

# Get the actual checkstyle-files-generator version
GENERATOR_VERSION=$(getPomVersion)

# Generate xdoc files with new version using property override
cd .ci-temp
./mvnw --no-transfer-progress clean process-classes \
  -Dcheckstyle-files-generator.version="${GENERATOR_VERSION}"

# Check if any files changed in whole repository
if [ "$(git status | grep 'Changes not staged\|Untracked files')" ]; then
  echo "Changes detected in checkstyle repository after xdoc generation."
  echo "Git status output:"
  git status
  echo "Top 300 lines of diff:"
  git diff | head -n 300
  exit 1
fi
