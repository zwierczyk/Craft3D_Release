#!/usr/bin/env bash
# Uruchomienie Craft3D Modern (wymaga okna graficznego).
# Przy pierwszym starcie rozpakuje assety (5 MB) do assets/minecraft.
set -euo pipefail
cd "$(dirname "$0")"

JAVA_BIN=""
if command -v java >/dev/null 2>&1; then
    JAVA_BIN=java
else
    TC="${CRAFT3D_TC_SH:-/home/user/craft3d-toolchain.sh}"
    if [ -f "$TC" ]; then source "$TC"; JAVA_BIN="$JAVA"; else
        echo "[run] brak java w PATH (zainstaluj Jave lub podaj CRAFT3D_TC_SH)"; exit 1
    fi
fi

[ -d out/classes ] || ./build.sh >/dev/null

"$JAVA_BIN" -cp "out/classes:lib/*" craft3dmodern.test.AssetsExtractor
exec "$JAVA_BIN" -cp "out/classes:lib/*" craft3dmodern.Main "$@"
