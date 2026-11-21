#!/usr/bin/env bash
set -euo pipefail

export LC_ALL=C.UTF-8
export LANG=C.UTF-8

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "=== بناء تطبيق معرب (APK) على لينكس Kubuntu ==="

# 1) فحص Java >= 17 والأدوات الأساسية
NEED_JAVA=0
if ! command -v java >/dev/null 2>&1; then
  NEED_JAVA=1
else
  JAVA_MAJOR="$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print ($1 == "1" ? $2 : $1)}')"
  if [ -z "${JAVA_MAJOR:-}" ] || [ "$JAVA_MAJOR" -lt 17 ] 2>/dev/null; then
    NEED_JAVA=1
  fi
fi

if [ "$NEED_JAVA" -eq 1 ] || ! command -v unzip >/dev/null 2>&1 || ! command -v wget >/dev/null 2>&1 || ! command -v curl >/dev/null 2>&1; then
  echo "[1/5] تثبيت OpenJDK 21 و unzip و wget و curl..."
  sudo apt-get update
  sudo apt-get install -y openjdk-21-jdk unzip wget curl
fi

if [ -d /usr/lib/jvm/java-21-openjdk-amd64 ]; then
  export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
  export PATH="$JAVA_HOME/bin:$PATH"
fi

# 2) فك أو إنشاء مفتاح التوقيع التجريبي
if [ ! -f "$PROJECT_DIR/debug.keystore" ]; then
  if [ -f "$PROJECT_DIR/debug.keystore.base64" ]; then
    echo "[2/5] تجهيز debug.keystore..."
    base64 -d "$PROJECT_DIR/debug.keystore.base64" > "$PROJECT_DIR/debug.keystore"
  else
    echo "[2/5] إنشاء debug.keystore..."
    keytool -genkeypair -noprompt \
      -storetype JKS \
      -keystore "$PROJECT_DIR/debug.keystore" \
      -storepass android \
      -alias androiddebugkey \
      -keypass android \
      -keyalg RSA \
      -keysize 2048 \
      -sigalg SHA256withRSA \
      -validity 10000 \
      -dname "CN=Android Debug,O=Android,C=US"
  fi
fi

# 3) تجهيز Android SDK 36.1
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

SDKMANAGER_BIN=""
if [ -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
  SDKMANAGER_BIN="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
else
  FOUND_SDKMGR="$(find "$ANDROID_HOME/cmdline-tools" -maxdepth 3 -type f -name sdkmanager 2>/dev/null | head -n 1 || true)"
  if [ -n "$FOUND_SDKMGR" ] && [ -x "$FOUND_SDKMGR" ]; then
    SDKMANAGER_BIN="$FOUND_SDKMGR"
  fi
fi

if [ -z "$SDKMANAGER_BIN" ]; then
  echo "[3/5] تنزيل Android Command-line Tools..."
  mkdir -p "$ANDROID_HOME/cmdline-tools"

  CMDL_URL="$(curl -fsSL https://developer.android.com/studio 2>/dev/null | grep -oE 'https://dl\.google\.com/android/repository/commandlinetools-linux-[0-9]+_latest\.zip' | head -n 1 || true)"
  CANDIDATE_URLS=()
  if [ -n "$CMDL_URL" ]; then
    CANDIDATE_URLS+=("$CMDL_URL")
  fi
  CANDIDATE_URLS+=(
    "https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip"
    "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
    "https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip"
  )

  DOWNLOADED=0
  for url in "${CANDIDATE_URLS[@]}"; do
    echo "    تجربة: $url"
    if curl -fL --progress-bar -o /tmp/cmdline-tools.zip "$url"; then
      DOWNLOADED=1
      break
    fi
  done

  if [ "$DOWNLOADED" -ne 1 ]; then
    echo "❌ تعذر تنزيل Android Command-line Tools. تأكد من الاتصال بالإنترنت أو عطّل VPN وجرّب مجدداً."
    exit 1
  fi

  rm -rf /tmp/cmdline-unpack
  unzip -q /tmp/cmdline-tools.zip -d /tmp/cmdline-unpack
  rm -rf "$ANDROID_HOME/cmdline-tools/latest"
  mv /tmp/cmdline-unpack/cmdline-tools "$ANDROID_HOME/cmdline-tools/latest"
  rm -rf /tmp/cmdline-tools.zip /tmp/cmdline-unpack
  SDKMANAGER_BIN="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
fi

export PATH="$(dirname "$SDKMANAGER_BIN"):$ANDROID_HOME/platform-tools:$PATH"

if [ ! -d "$ANDROID_HOME/platforms/android-36.1" ] || [ ! -d "$ANDROID_HOME/platforms/android-36" ]; then
  echo "[4/5] تثبيت Android SDK 36.1..."
  set +o pipefail
  yes | "$SDKMANAGER_BIN" --sdk_root="$ANDROID_HOME" --licenses >/dev/null
  set -o pipefail
  "$SDKMANAGER_BIN" --sdk_root="$ANDROID_HOME" "platform-tools" "platforms;android-36.1" "platforms;android-36" "build-tools;36.0.0"
else
  echo "[4/5] Android SDK 36.1 جاهز."
fi

# ربط مسار SDK محلياً على جهاز المستخدم
echo "sdk.dir=$ANDROID_HOME" > "$PROJECT_DIR/local.properties"

# 4) تجهيز Gradle 9.3.1 محلياً في مجلد المستخدم
GRADLE_DIR="$HOME/.local/share/gradle-9.3.1"
GRADLE_BIN="$GRADLE_DIR/bin/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
  echo "[5/5] تنزيل Gradle 9.3.1..."
  mkdir -p "$HOME/.local/share"
  curl -fL --progress-bar -o /tmp/gradle-9.3.1-bin.zip \
    "https://services.gradle.org/distributions/gradle-9.3.1-bin.zip"
  unzip -q /tmp/gradle-9.3.1-bin.zip -d "$HOME/.local/share"
  rm -f /tmp/gradle-9.3.1-bin.zip
fi

# حذف أي ملف APK وهمي حجمه 0 ناتج عن تصدير المنصة قبل البناء
find "$PROJECT_DIR/apks" -maxdepth 1 -type f -name "*.apk" -empty -delete 2>/dev/null || true

echo "=== جاري بناء ملف APK ==="
export JAVA_TOOL_OPTIONS="-Djava.net.preferIPv4Stack=true ${JAVA_TOOL_OPTIONS:-}"
"$GRADLE_BIN" --stop >/dev/null 2>&1 || true

PUBLIC_SHARE_URL="${PUBLIC_SHARE_URL:-}"
if [ -z "$PUBLIC_SHARE_URL" ] && command -v git >/dev/null 2>&1 && git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  ORIGIN_URL="$(git remote get-url origin 2>/dev/null || true)"
  if [[ "$ORIGIN_URL" =~ github\.com[:/]([^/]+)/([^/.]+)(\.git)?$ ]]; then
    GH_OWNER="${BASH_REMATCH[1]}"
    GH_REPO="${BASH_REMATCH[2]}"
    if [ "$GH_REPO" = "${GH_OWNER}.github.io" ]; then
      PUBLIC_SHARE_URL="https://${GH_OWNER}.github.io/"
    else
      PUBLIC_SHARE_URL="https://${GH_OWNER}.github.io/${GH_REPO}/"
    fi
  fi
fi

BUILD_OK=0
for attempt in 1 2 3; do
  echo "محاولة البناء ($attempt/3)..."
  if "$GRADLE_BIN" :app:assembleDebug \
    -PpublicShareUrl="$PUBLIC_SHARE_URL" \
    --max-workers=2 \
    -Djava.net.preferIPv4Stack=true \
    -Dorg.gradle.internal.http.connectionTimeout=120000 \
    -Dorg.gradle.internal.http.socketTimeout=120000; then
    BUILD_OK=1
    break
  fi
  echo "⚠️ تعثر تنزيل بعض الملفات من الشبكة، سيتم التوقف 5 ثوانٍ ثم المتابعة..."
  sleep 5
done

if [ "$BUILD_OK" -ne 1 ]; then
  echo "❌ فشل البناء بعد 3 محاولات. تأكد من استقرار الإنترنت أو استخدم DNS مثل 8.8.8.8 ثم أعد تشغيل السكربت."
  exit 1
fi

mkdir -p "$PROJECT_DIR/docs"
APK_OUT="$(find "$PROJECT_DIR/apks" -maxdepth 1 -type f -name "*.apk" | head -n 1 || true)"
if [ -n "$APK_OUT" ] && [ -f "$APK_OUT" ]; then
  cp -f "$APK_OUT" "$PROJECT_DIR/docs/muerib.apk"
fi
if [ -f "$PROJECT_DIR/app/src/main/res/drawable/ic_launcher_muerib_clean.png" ]; then
  cp -f "$PROJECT_DIR/app/src/main/res/drawable/ic_launcher_muerib_clean.png" "$PROJECT_DIR/docs/icon.png"
fi

echo ""
echo "✅ تم البناء بنجاح! ملف الـ APK الحقيقي:"
ls -lh "$PROJECT_DIR"/apks/*.apk
echo "🌐 مجلد صفحة التحميل العامة جاهز للنشر في: $PROJECT_DIR/docs"

