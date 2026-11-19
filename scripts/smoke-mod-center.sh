#!/usr/bin/env bash
set -euo pipefail
apk=AndroidLauncher/app/build/outputs/apk/debug/app-debug.apk
test -s "$apk"
adb install -r "$apk"
echo "--- Launch main ---"
adb shell am start -W -n com.geko7490.saengineandroid/.MainActivity
sleep 4
echo "--- Launch Mod Center without GTA or Shizuku installed ---"
adb shell am start -W -n com.geko7490.saengineandroid/.ModCenterActivity
sleep 5
echo "--- Confirm visible activity ---"
adb shell dumpsys activity activities | grep -F 'ModCenterActivity'
echo "--- Verify no application crash ---"
adb shell logcat -d -v brief > /tmp/mod-center-logcat.txt
if grep -A 22 'FATAL EXCEPTION' /tmp/mod-center-logcat.txt | grep -F 'com.geko7490.saengineandroid'; then
  echo "::error::SA Engine Mod Center crashed" >&2
  exit 1
fi
echo "PASS: Android 35 Mod Center launched without Shizuku installed"
