#!/usr/bin/env bash
# Build and package Contact Front (tactical mode) into a native app image via jpackage.
#
# One JVM, no network, no bundled interpreter. Builds the Maven reactor, assembles an input
# directory of jars (app + engine + org.json + JavaFX), then runs jpackage to emit a
# double-clickable app image. Run this ON the target OS (jpackage does not cross-compile).
#
# Requires: maven on PATH and a JDK 21+ whose bin/ contains jpackage.
# Offline build is used (-o) since all dependencies are cached in the local Maven repo.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
M2="$HOME/.m2/repository"
MVN="${MVN:-}"
if [ -z "$MVN" ] && [ -x "$ROOT/tools/apache-maven-3.9.16/bin/mvn" ]; then
  MVN="$ROOT/tools/apache-maven-3.9.16/bin/mvn"
elif [ -z "$MVN" ]; then
  MVN="mvn"
fi
APP_VER="${APP_VER:-1.0.0}"
FX_VER="${FX_VER:-21.0.2}"

if [[ "$(uname)" == *"MINGW"* || "$(uname)" == "MSYS"* || "$(uname)" == "CYGWIN"* ]]; then
  PLATFORM="win"; EXT=".exe"
elif [[ "$(uname)" == "Darwin" ]]; then
  PLATFORM="mac"; EXT=""
else
  PLATFORM="linux"; EXT=""
fi

JPKG="${JAVA_HOME:+${JAVA_HOME}/bin/}jpackage"

OUT="$ROOT/build/input"
DEST="$ROOT/dist"
mkdir -p "$OUT" "$DEST"

echo "==> Building Maven reactor (offline)"
( cd "$ROOT" && "$MVN" -o -DskipTests package )

copy_jar() {
  local src="$1"
  [ -f "$src" ] || { echo "Missing dependency jar: $src" >&2; exit 1; }
  cp -f "$src" "$OUT"
  echo "    + $(basename "$src")"
}

echo "==> Setting Main-Class on UI jar"
jar ufe "$ROOT/ui/target/contact-front-ui-$APP_VER.jar" com.contactfront.ui.App

echo "==> Assembling jpackage input ($PLATFORM)"
copy_jar "$ROOT/ui/target/contact-front-ui-$APP_VER.jar"
copy_jar "$ROOT/engine/target/contact-front-engine-$APP_VER.jar"
copy_jar "$M2/org/json/json/20240303/json-20240303.jar"
for m in base controls graphics; do
  copy_jar "$M2/org/openjfx/javafx-$m/$FX_VER/javafx-$m-$FX_VER.jar"
  copy_jar "$M2/org/openjfx/javafx-$m/$FX_VER/javafx-$m-$FX_VER-$PLATFORM.jar"
done

ICON_ARG=()
if [[ "$PLATFORM" == "win" && -f "$ROOT/assets/icon.ico" ]]; then
  ICON_ARG=(--icon "$ROOT/assets/icon.ico")
elif [[ -f "$ROOT/assets/icon.png" ]]; then
  ICON_ARG=(--icon "$ROOT/assets/icon.png")
fi

# app-image has no installer-specific options (shortcuts apply to exe/msi/pkg/dmg).
PLATFORM_OPTS=()

# Bundle the full JDK as the runtime image. App jars (incl. modular JavaFX jars) go on the
# classpath, where JavaFX loads as automatic modules -- the same setup verified to launch.
# Avoids offline jlink module-resolution issues.
if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "JAVA_HOME must be set to a JDK 21+ (used as the bundled runtime)." >&2
  exit 1
fi
RUNTIME_IMAGE="$JAVA_HOME"

APP_IMAGE="$DEST/Contact Front"
[ -d "$APP_IMAGE" ] && rm -rf "$APP_IMAGE"

echo "==> Packaging app image (jpackage)"
"$JPKG" --type app-image \
  --name "Contact Front" \
  --runtime-image "$RUNTIME_IMAGE" \
  --input "$OUT" \
  --main-jar "contact-front-ui-$APP_VER.jar" \
  --main-class com.contactfront.ui.App \
  --app-version "$APP_VER" \
  --dest "$DEST" \
  --java-options "--module-path \$APPDIR --add-modules javafx.controls,javafx.graphics --enable-native-access=javafx.graphics" \
  "${ICON_ARG[@]}" "${PLATFORM_OPTS[@]}"

echo "==> App image written to $DEST"
