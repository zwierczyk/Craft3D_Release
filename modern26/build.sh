#!/usr/bin/env bash
# ============================================================
#  Craft3D Modern - build
#  Uzywa systemowego javac, gdy jest; w sandboxie (bez Javy)
#  wraca do narzedzi z craft3d-toolchain.sh (ECJ).
# ============================================================
set -euo pipefail
cd "$(dirname "$0")"

rm -rf out && mkdir -p out/classes
find src -name "*.java" > out/sources.txt

if command -v javac >/dev/null 2>&1 && [ "${CRAFT3D_USE_ECJ:-0}" != "1" ]; then
    echo "[build] javac: $(javac -version 2>&1)"
    javac -encoding UTF-8 -cp "$(echo lib/*.jar | tr ' ' ':')" -d out/classes @out/sources.txt
else
    TC="${CRAFT3D_TC_SH:-/home/user/craft3d-toolchain.sh}"
    if [ -f "$TC" ]; then source "$TC"; else
        echo "[build] brak javac i brak toolchaina ($TC)"; exit 1
    fi
    echo "[build] ECJ:"
    "$JAVA" -cp "$ECJ" org.eclipse.jdt.internal.compiler.batch.Main \
        -source 8 -target 8 -nowarn \
        -cp "$(echo lib/*.jar | tr ' ' ':')" -d out/classes @out/sources.txt
fi

echo "[build] DONE -> out/classes"
