#!/usr/bin/env bash
# ============================================================
#  Craft3D - Linux / macOS launcher
#  Wymagania: Java 11 lub nowsza
# ============================================================

cd "$(dirname "$0")"

# Sprawdz Jave
if ! command -v java >/dev/null 2>&1; then
    echo ""
    echo " [BLAD] Java nie jest zainstalowana lub nie ma jej w PATH."
    echo " Linux:  sudo apt install openjdk-17-jdk"
    echo " macOS:  brew install openjdk@17"
    echo ""
    exit 1
fi

# Na macOS LWJGL/GLFW wymaga -XstartOnFirstThread
JVM_ARGS=""
if [[ "$OSTYPE" == "darwin"* ]]; then
    JVM_ARGS="-XstartOnFirstThread"
fi

java $JVM_ARGS -cp "Craft3D.jar:lib/*" craft3dgl.MinecraftGL
