#!/bin/sh
# ─────────────────────────────────────────────────────────────────────────────
# Maven Wrapper startup script for UNIX-like systems.
# Automatically downloads Apache Maven 3.9.6 if not already present.
# ─────────────────────────────────────────────────────────────────────────────

set -e

MAVEN_VERSION="3.9.6"
MAVEN_HOME="${HOME}/.m2/wrapper/dists/apache-maven-${MAVEN_VERSION}-bin/apache-maven-${MAVEN_VERSION}"
MAVEN_DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
MAVEN_ARCHIVE="/tmp/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
MAVEN_DEST="${HOME}/.m2/wrapper/dists/apache-maven-${MAVEN_VERSION}-bin"

if [ ! -f "${MAVEN_HOME}/bin/mvn" ]; then
  echo "Downloading Apache Maven ${MAVEN_VERSION}..."
  mkdir -p "${MAVEN_DEST}"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "${MAVEN_DOWNLOAD_URL}" -o "${MAVEN_ARCHIVE}"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "${MAVEN_DOWNLOAD_URL}" -O "${MAVEN_ARCHIVE}"
  else
    echo "ERROR: Neither curl nor wget is available. Please install Maven manually." >&2
    exit 1
  fi
  tar -xzf "${MAVEN_ARCHIVE}" -C "${MAVEN_DEST}"
  rm -f "${MAVEN_ARCHIVE}"
  echo "Maven ${MAVEN_VERSION} installed to ${MAVEN_HOME}"
fi

exec "${MAVEN_HOME}/bin/mvn" "$@"
