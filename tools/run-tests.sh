#!/usr/bin/env bash
#
# Builds FlowForge and runs the full JUnit 5 test suite using only the JUnit
# jars in the local Maven repository (~/.m2). This lets the project be built
# and verified from the command line even though Maven/Gradle are not
# installed on this machine. IntelliJ users can ignore this and use the
# bundled pom.xml instead.
#
set -euo pipefail

cd "$(dirname "$0")/.."
ROOT="$(pwd)"
M2="${HOME}/.m2/repository"
OUT_MAIN="${ROOT}/out/main"
OUT_TEST="${ROOT}/out/test"

JUNIT_VERSION="5.8.2"
PLATFORM_VERSION="1.8.2"

CP_JARS=(
  "${M2}/org/junit/jupiter/junit-jupiter-api/${JUNIT_VERSION}/junit-jupiter-api-${JUNIT_VERSION}.jar"
  "${M2}/org/junit/jupiter/junit-jupiter-engine/${JUNIT_VERSION}/junit-jupiter-engine-${JUNIT_VERSION}.jar"
  "${M2}/org/junit/jupiter/junit-jupiter-params/${JUNIT_VERSION}/junit-jupiter-params-${JUNIT_VERSION}.jar"
  "${M2}/org/junit/platform/junit-platform-commons/${PLATFORM_VERSION}/junit-platform-commons-${PLATFORM_VERSION}.jar"
  "${M2}/org/junit/platform/junit-platform-engine/${PLATFORM_VERSION}/junit-platform-engine-${PLATFORM_VERSION}.jar"
  "${M2}/org/junit/platform/junit-platform-launcher/${PLATFORM_VERSION}/junit-platform-launcher-${PLATFORM_VERSION}.jar"
  "${M2}/org/opentest4j/opentest4j/1.2.0/opentest4j-1.2.0.jar"
  "${M2}/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar"
)

JUNIT_CP="$(IFS=:; echo "${CP_JARS[*]}")"

# The SQLite JDBC driver bundled in lib/ (needed to compile/run the SQLite
# repository and its tests).
LIB_CP="$(printf '%s' "$(ls "${ROOT}"/lib/*.jar 2>/dev/null | tr '\n' ':')")"

echo "==> Checking JUnit jars in ${M2}"
for jar in "${CP_JARS[@]}"; do
  if [[ ! -f "${jar}" ]]; then
    echo "    MISSING: ${jar}" >&2
    echo "    Run a project that pulls JUnit ${JUNIT_VERSION} first, or install the jars." >&2
    exit 2
  fi
done

echo "==> Compiling main sources"
rm -rf "${OUT_MAIN}" && mkdir -p "${OUT_MAIN}"
find src/main/java -name '*.java' > /tmp/flowforge_main_sources.txt
javac -cp "${LIB_CP}" -d "${OUT_MAIN}" @/tmp/flowforge_main_sources.txt

echo "==> Copying non-Java resources (fonts, etc.)"
( cd src/main/java && find . -type f ! -name '*.java' -print0 \
    | while IFS= read -r -d '' f; do
        mkdir -p "${OUT_MAIN}/$(dirname "$f")"
        cp "$f" "${OUT_MAIN}/$f"
      done )

echo "==> Compiling test sources + runner"
rm -rf "${OUT_TEST}" && mkdir -p "${OUT_TEST}"
find src/test/java -name '*.java' > /tmp/flowforge_test_sources.txt
echo "tools/JUnitRunner.java" >> /tmp/flowforge_test_sources.txt
javac -cp "${OUT_MAIN}:${JUNIT_CP}:${LIB_CP}" -d "${OUT_TEST}" @/tmp/flowforge_test_sources.txt

echo "==> Running tests"
java -cp "${OUT_TEST}:${OUT_MAIN}:${JUNIT_CP}:${LIB_CP}" JUnitRunner
