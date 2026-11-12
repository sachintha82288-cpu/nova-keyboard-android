#!/usr/bin/env bash
# Sets up a working APK build/analysis toolchain in a sandbox where the Android SDK,
# Gradle and Maven hosts are NOT reachable. Everything comes from PyPI + npm, which
# are reachable, and is unpacked under .toolchain/ (git-ignored).
#
#   bash scripts/setup_apk_toolchain.sh          # install
#   source .toolchain/env.sh                     # put java/aapt2/apktool on PATH
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TC="$ROOT/.toolchain"
VENV="$TC/venv"
mkdir -p "$TC"

echo "== python venv + analysis libraries"
python3 -m venv "$VENV"
"$VENV/bin/pip" install -q --upgrade pip
"$VENV/bin/pip" install -q androguard pyaxmlparser asn1crypto apksigtool aapt2 "jdk4py==17.0.9.2"

JAVA_HOME="$("$VENV/bin/python" -c 'import jdk4py;print(jdk4py.JAVA_HOME)')"
AAPT2="$("$VENV/bin/python" - <<'PY'
import aapt2, pathlib
print(pathlib.Path(aapt2.__file__).parent / "bin" / "Linux" / "aapt2")
PY
)"
chmod +x "$AAPT2"

echo "== apktool (from the npm registry tarball)"
if [ ! -f "$TC/apktool.jar" ]; then
  curl -sSL -o "$TC/apktool.tgz" https://registry.npmjs.org/apktool-jar/-/apktool-jar-2.4.1.tgz
  tar xzf "$TC/apktool.tgz" -C "$TC" --strip-components=2 package/bin/apktool_2.4.1.jar
  mv "$TC/apktool_2.4.1.jar" "$TC/apktool.jar"
  rm -f "$TC/apktool.tgz"
fi

cat > "$TC/env.sh" <<ENV
export NOVA_TOOLCHAIN="$TC"
export JAVA_HOME="$JAVA_HOME"
export PATH="$JAVA_HOME/bin:$TC/bin:\$PATH"
export AAPT2="$AAPT2"
alias apktool='java -jar "\$NOVA_TOOLCHAIN/apktool.jar"'
ENV
mkdir -p "$TC/bin"
cat > "$TC/bin/apktool" <<WRAP
#!/usr/bin/env bash
exec "$JAVA_HOME/bin/java" -jar "$TC/apktool.jar" "\$@"
WRAP
cat > "$TC/bin/aapt2" <<WRAP
#!/usr/bin/env bash
exec "$AAPT2" "\$@"
WRAP
chmod +x "$TC/bin/apktool" "$TC/bin/aapt2"

echo
echo "installed:"
"$JAVA_HOME/bin/java" -version 2>&1 | head -1
"$AAPT2" version
"$JAVA_HOME/bin/java" -jar "$TC/apktool.jar" --version
"$VENV/bin/python" -c 'import androguard;print("androguard", androguard.__version__)'
echo
echo "now run:  source .toolchain/env.sh"
