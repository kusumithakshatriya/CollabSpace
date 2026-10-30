#!/usr/bin/env sh
set -eu
if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven not found. Install Java 17 and Maven, or use Docker: docker compose up --build"
  exit 1
fi
mvn spring-boot:run
