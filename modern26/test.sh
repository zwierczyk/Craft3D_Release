#!/usr/bin/env bash
# Testy headless (bez okna GL). Najpierw rozpakowuje assety i kompiluje wszystko.
set -euo pipefail
cd "$(dirname "$0")"

JAVA_BIN=""
if command -v java >/dev/null 2>&1; then
    JAVA_BIN=java
else
    TC="${CRAFT3D_TC_SH:-/home/user/craft3d-toolchain.sh}"
    if [ -f "$TC" ]; then source "$TC"; JAVA_BIN="$JAVA"; else
        echo "[test] brak java w PATH (zainstaluj Jave lub podaj CRAFT3D_TC_SH)"; exit 1
    fi
fi

./build.sh >/dev/null

"$JAVA_BIN" -cp "out/classes:lib/*" craft3dmodern.test.AssetsExtractor

"$JAVA_BIN" -cp "out/classes:lib/*" craft3dmodern.test.AssetsTest
"$JAVA_BIN" -cp "out/classes:lib/*" craft3dmodern.test.FontTest
"$JAVA_BIN" -cp "out/classes:lib/*" craft3dmodern.test.WorldTest
"$JAVA_BIN" -cp "out/classes:lib/*" craft3dmodern.test.GameplayTest
