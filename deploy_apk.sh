#!/bin/bash
set -e
cd "/home/walzetass-kar/Documents/ProjectIhwal/CariKostKita_FastAPI_Docs/CariKostKita mobile app"

echo "1. Building release APK..."
./gradlew assembleRelease

echo "2. Locating APK..."
APK_PATH=$(find app/build/outputs/apk/release -name "*.apk" 2>/dev/null | head -n 1)
if [ -z "$APK_PATH" ]; then
    APK_PATH=$(find app/build -name "*release*.apk" 2>/dev/null | head -n 1)
fi

if [ -z "$APK_PATH" ]; then
    echo "ERROR: APK not found!"
    find app/build
    exit 1
fi
echo "Found APK at: $APK_PATH"

echo "3. Copying APK..."
cp -v "$APK_PATH" CariKostKita-v1.3.5-release.apk
cp -v "$APK_PATH" CariKostKita-v1.3.4-release.apk
cp -v "$APK_PATH" CariKostKita-v1.3.3-release.apk
cp -v "$APK_PATH" CariKostKita-v1.3.2-release.apk
cp -v "$APK_PATH" CariKostKita-v1.3.1-release.apk
cp -v "$APK_PATH" "/home/walzetass-kar/Documents/ProjectIhwal/CariKostKita_FastAPI_Docs/Landing Page/public/releases/CariKostKita-v1.3.5-release.apk"
cp -v "$APK_PATH" "/home/walzetass-kar/Documents/ProjectIhwal/CariKostKita_FastAPI_Docs/Landing Page/public/releases/CariKostKita-v1.3.4-release.apk"
cp -v "$APK_PATH" "/home/walzetass-kar/Documents/ProjectIhwal/CariKostKita_FastAPI_Docs/Landing Page/public/releases/CariKostKita-v1.3.3-release.apk"
cp -v "$APK_PATH" "/home/walzetass-kar/Documents/ProjectIhwal/CariKostKita_FastAPI_Docs/Landing Page/public/releases/CariKostKita-v1.3.2-release.apk"
cp -v "$APK_PATH" "/home/walzetass-kar/Documents/ProjectIhwal/CariKostKita_FastAPI_Docs/Landing Page/public/releases/CariKostKita-v1.3.1-release.apk"

echo "4. Verifying APK signature..."
$ANDROID_HOME/build-tools/34.0.0/apksigner verify --print-certs CariKostKita-v1.3.5-release.apk

echo "5. Uploading to VPS..."
scp -i ~/.ssh/id_ed25519 CariKostKita-v1.3.5-release.apk root@194.62.248.20:/var/lib/docker/volumes/carikostkita_media_data/_data/releases/CariKostKita-v1.3.5-release.apk
ssh -i ~/.ssh/id_ed25519 root@194.62.248.20 "cd /var/lib/docker/volumes/carikostkita_media_data/_data/releases/ && cp -v CariKostKita-v1.3.5-release.apk CariKostKita-v1.3.4-release.apk && cp -v CariKostKita-v1.3.5-release.apk CariKostKita-v1.3.3-release.apk && cp -v CariKostKita-v1.3.5-release.apk CariKostKita-v1.3.2-release.apk && cp -v CariKostKita-v1.3.5-release.apk CariKostKita-v1.3.1-release.apk"

echo "ALL DONE!"
