#!/usr/bin/env bash
# Testy headless (bez okna GL). Najpierw rozpakowuje assety.
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

[ -d out/classes ] || ./build.sh >/dev/null

"$JAVA_BIN" -cp "out/classes:lib/*" craft3dmodern.test.AssetsExtractor

mkdir -p out/testclasses
find src -path "*test/*.java" > out/test-sources.txt

if command -v javac >/dev/null 2>&1; then
    javac -encoding UTF-8 -cp "out/classes" -d out/testclasses @out/test-sources.txt
else
    if [ -f "$TC" ]; then source "$TC"; else
        echo "[test] brak javac i brak toolchaina"; exit 1
    fi
    "$JAVA" -cp "$ECJ" org.eclipse.jdt.internal.compiler.batch.Main \
        -source 8 -target 8 -nowarn -cp "out/classes" -d out/testclasses @out/test-sources.txt
fi

"$JAVA_BIN" -cp "out/classes:out/testclasses:lib/*" craft3dmodern.test.AssetsTest
"$JAVA_BIN" -cp "out/classes:out/testclasses:lib/*" craft3dmodern.test.FontTest
"$JAVA_BIN" -cp "out/classes:out/testclasses:lib/*" craft3dmodern.test.WorldTest
