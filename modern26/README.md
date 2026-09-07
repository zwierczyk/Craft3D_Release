# Craft3D Modern (wewnatrz repozytorium Craft3D_Release)

Nowoczesny silnik wzorowany na **Minecraft Java Edition 26.2** (najnowszy
release, 16.06.2026): OpenGL 3.3 core-profile + shadery, prawdziwe assety
vanilla 26.2. Stary projekt (katalog glowny repo) pozostaje wierny erze
1.12 / MCP 9.40 — ten katalog (`modern26/`) to osobny, nowy silnik.

## IntelliJ (szybki start)

Modul projektu (`Craft3D_Release.iml`) ma juz zarejestrowane `modern26/src`
jako zrodla i `modern26/lib` jako biblioteke. Po `git pull`:

1. Jesli IntelliJ nie przeladowal modulu: prawy przycisk na projekcie →
   `Reload from Disk` (albo `File -> Reload All from Disk`), ewentualnie
   `File -> Invalidate Caches / Restart`.
2. Run/Debug Configuration: main class `craft3dmodern.Main`, working dir
   moze byc dowolny (assety wykrywaja sie same, przy starcie nastepuje
   auto-rozpakowanie `minecraft-26.2.zip`).
3. Jesli pliki nadal widac na pomaranczowo (folder nieuznany za zrodla):
   prawy przycisk na `modern26/src` → `Mark Directory as` → `Sources Root`.


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
    util/Json.java       # minimalny, samowystarczalny parser JSON
    client/
      Game.java          # ekrany: tytulowy / wybor swiata / opcje
      I18n.java          # en_us z assetow + PL dla uzywanych kluczy
      font/FontRenderer  # font 26.2 z default.json (layout wg BitmapProvider)
      ui/Button.java     # przycisk vanilla 26.2 (nine-slice border z .mcmeta)
    render/              # ShaderProgram, Texture, GuiBlit (tint/rect)
    test/                # AssetsExtractor, AssetsTest, FontTest (headless)
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
- [x] M2: font z assetow (proporcjonalny, PL), przyciski nine-slice, I18n
      (en_us + PL), splash, ekran tytulowy / wybor swiata / opcje, mysz i ESC
- [ ] M3: rejestr blokow z assetow (modele+blockstate'y), chunk mesh
- [ ] M4: gracz, fizyka, interakcje
