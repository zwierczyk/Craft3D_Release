# Craft3D Modern (wewnatrz repozytorium Craft3D_Release)

Nowoczesny silnik wzorowany na **Minecraft Java Edition 26.2** (najnowszy
release, 16.06.2026): OpenGL 3.3 core-profile + shadery, prawdziwe assety
vanilla 26.2. Stary projekt (katalog glowny repo) pozostaje wierny erze
1.12 / MCP 9.40 — ten katalog (`modern26/`) to osobny, nowy silnik.

## Szybki start

Wymagania: **Java JDK 8+** (IntelliJ ma wlasna; jesli `javac`/`java` sa w
PATH, skrypty uzyja ich automatycznie).

Linux / macOS:
```bash
cd modern26
./build.sh     # kompilacja -> out/classes
./test.sh      # testy headless (assetow) - opcjonalne
./run.sh       # uruchomienie gry (wymaga ekranu)
```

Windows (wiersz polecenia):
```bat
cd modern26
build.bat
test.bat
run.bat
```

Przy pierwszym uruchomieniu assety (5 MB, `assets/minecraft-26.2.zip`)
zostana automatycznie rozpakowane do `assets/minecraft/` (katalog jest w
.gitignore, nie wchodzi do gita).

## Struktura

```
modern26/
  src/craft3dmodern/
    Main.java            # okno GLFW + petla (GL 3.3 CORE)
    client/Game.java     # stan gry / proof-of-life ekranu tytulowego
    render/              # ShaderProgram, Texture, GuiBlit
    test/                # AssetsExtractor, AssetsTest (headless)
  assets/
    minecraft-26.2.zip   # ORYGINALNE assety 26.2 (tekstury, modele, lang, font)
    pack.png, version.json
  lib/                   # LWJGL 3.3.3 (+ natywne: linux/macos/windows)
```

## Skad assety i zrodla odniesienia

- Assety 26.2: oryginalny jar klienta 26.2 (`version.json` = 26.2,
  world_version 4903) - tutaj spakowany do `minecraft-26.2.zip`.
- Zrodla 26.2 do porownan (dekompilacja, oficjalne mapowania):
  `docs/official-262` w repo GitHub `366862732/DirectXmod` (do pobrania
  lokalnie: `git clone --filter=blob:none --no-checkout
  https://github.com/366862732/DirectXmod.git && git sparse-checkout set
  docs/official-262/net/minecraft/client`).
- Uwaga: w tym jarze obrazy panoramy (tlo menu) sa zminimalizowane do 1x1.
  Pelne 6 klatek `panorama_0..5.png` + `panorama_overlay.png` mozna wrzucic
  do `assets/minecraft/textures/gui/title/background/` (z dowolnego zrodla
  oryginalnych assetow 26.2).

## Kamienie milowe

- [x] M1: szkielet, GL 3.3 core, shader+tekstura, logo i przyciski 26.2
- [ ] M2: font + GuiGraphics, ekran tytulowy z napisami (TitleScreen 26.2)
- [ ] M3: rejestr blokow z assetow (modele+blockstate'y), chunk mesh
- [ ] M4: gracz, fizyka, interakcje
