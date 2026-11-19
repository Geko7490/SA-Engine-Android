#!/usr/bin/env bash
set -euo pipefail
apk=AndroidLauncher/app/build/outputs/apk/debug/app-debug.apk
test -s "$apk"
adb install -r "$apk"
echo "--- Launch main activity ---"
adb shell am start -W -n com.geko7490.saengineandroid/.MainActivity
sleep 4

echo "--- Inspect UI and find the actual Mod Center button ---"
adb shell uiautomator dump /sdcard/sa_mod_ui.xml
adb shell cat /sdcard/sa_mod_ui.xml > /tmp/sa_mod_ui.xml
coords="$(python3 - <<'PY'
import re,xml.etree.ElementTree as ET
root=ET.parse('/tmp/sa_mod_ui.xml').getroot()
for node in root.iter('node'):
    if 'GTA SA MOD CENTER' in node.attrib.get('text',''):
        b=node.attrib.get('bounds','')
        m=re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]',b)
        if not m: raise SystemExit('Mod Center button lacks bounds')
        a=list(map(int,m.groups()))
        print((a[0]+a[2])//2,(a[1]+a[3])//2)
        break
else:
    raise SystemExit('Mod Center button not found in screen hierarchy')
PY
)"
read -r x y <<< "$coords"
echo "Tapping Mod Center button x=$x y=$y"
adb shell input tap "$x" "$y"
sleep 6
echo "--- Verify that the foreground activity is Mod Center ---"
adb shell dumpsys activity activities | grep -F 'ModCenterActivity'
echo "--- Verify no app crash ---"
adb shell logcat -d -v brief > /tmp/mod-center-logcat.txt
if grep -A 22 'FATAL EXCEPTION' /tmp/mod-center-logcat.txt | grep -F 'com.geko7490.saengineandroid'; then
  echo "::error::SA Engine Mod Center crashed" >&2
  exit 1
fi
echo "PASS: Android 35 Mod Center opened through real UI without game/Shizuku installed"
