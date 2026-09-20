#!/usr/bin/env bash
# Signs (and zipaligns) a release APK with uber-apk-signer
# (https://github.com/patrickfav/uber-apk-signer), which stamps an APK with
# every signature scheme (v1/v2/v3) in one pass. That broad compatibility is
# why it's worth using here: many of the unofficial installer apps used on
# Android Auto/Automotive head units (KingsInstaller included) are far more
# forgiving of it than of a plain `apksigner` v2-only signature.
#
# This is entirely optional - Android Studio's own "Generate Signed Bundle /
# APK" flow works fine too. Use this only if you want a quick, scriptable,
# throwaway-keystore signed build to side-load onto a head unit for testing.
#
# Usage:
#   ./scripts/sign-release.sh path/to/app-release-unsigned.apk [extra uber-apk-signer args...]
#
# Requires a JRE and the uber-apk-signer jar. Point UBER_APK_SIGNER_JAR at a
# downloaded copy, or one will be looked for at tools/uber-apk-signer.jar.
# Grab a release jar from:
#   https://github.com/patrickfav/uber-apk-signer/releases

set -euo pipefail

APK_PATH="${1:?Usage: sign-release.sh <path-to-unsigned-apk> [extra args...]}"
shift || true

JAR_PATH="${UBER_APK_SIGNER_JAR:-tools/uber-apk-signer.jar}"

if [[ ! -f "$JAR_PATH" ]]; then
  echo "uber-apk-signer jar not found at '$JAR_PATH'." >&2
  echo "Download a release from https://github.com/patrickfav/uber-apk-signer/releases" >&2
  echo "and either place it at tools/uber-apk-signer.jar or set UBER_APK_SIGNER_JAR." >&2
  exit 1
fi

if [[ ! -f "$APK_PATH" ]]; then
  echo "APK not found: $APK_PATH" >&2
  exit 1
fi

# --allowResign lets this run again over an already-signed debug build;
# with no --ks given, uber-apk-signer generates (and reuses) its own
# throwaway debug-style keystore, which is fine for side-loading but not
# for anything you intend to keep updating long-term - pass your own --ks
# for that, and keep that keystore somewhere safe.
java -jar "$JAR_PATH" \
  --apks "$APK_PATH" \
  --allowResign \
  "$@"
