# Craft3D - Voxel Survival Game

Minecraft-podobna gra napisana w Javie z użyciem LWJGL/OpenGL 2.1.

**Najszybszy start:** zobacz plik `URUCHOM_MNIE.txt`.

## Wymagania

- **Java 11+** ([Adoptium / Eclipse Temurin](https://adoptium.net/))

To wszystko! Wszystkie biblioteki są w folderze `lib/`.

## Uruchomienie

| System | Polecenie |
|--------|-----------|
| Windows | dwuklik `start_windows.bat` |
| Linux | `./start_linux_mac.sh` |
| macOS | `./start_linux_mac.sh` |

Albo ręcznie z terminala:
```bash
java -cp "Craft3D.jar:lib/*" craft3dgl.MinecraftGL
```
(na Windows użyj `;` zamiast `:`)

## Sterowanie

| Klawisz | Akcja |
|---------|-------|
| `WASD` | Ruch |
| `Space` | Skok (lub w górę przy lataniu) |
| `Shift` | Sprint / w dół przy lataniu |
| `Ctrl` | Sprint przy lataniu (szybciej) |
| `LPM` | Kop blok / atakuj |
| `PPM` | Postaw blok / interakcja / zjedz |
| `1-9` | Wybór hotbara |
| `Scroll` | Zmiana slotu hotbara |
| `E` | Otwórz inventory |
| `Q` | Wyrzuć przedmiot (Shift+Q = cały stack) |
| `T` | Otwórz chat |
| `/` | Otwórz chat z `/` (komendy) |
| `F5` | Zmień widok kamery |
| `M` | Wycisz/odcisz |
| `ESC` | Menu pauzy |
| `Space x2` (creative) | Włącz/wyłącz latanie |

## Komendy chatu

- `/gamemode <c|s>` - tryb creative/survival
- `/fly` - przełącz latanie (creative)
- `/tp <x> <y> <z>` - teleport
- `/give <nazwa> [ilość]` - daj przedmiot
- `/time set <day|night|tick>` - ustaw porę dnia
- `/weather <clear|rain|thunder>` - ustaw pogodę
- `/fps` - pokaż liczbę klatek na sekundę
- `/help` - pomoc

## Co naprawione

- ✅ Skrzynki działają (można otwierać/zamykać)
- ✅ Villagerzy spawnują się tylko w wioskach (raz na wioskę)
- ✅ Domki w wioskach mają pełne 4 ściany + dach + drzwi z framugą
- ✅ Ładniejszy model villagera z 3 profesjami (Farmer/Librarian/Toolsmith)
- ✅ Villagerzy wracają do swojej wioski

## Struktura projektu

```
Craft3D/
├── Craft3D.jar              ← skompilowana gra
├── MANIFEST.MF              ← manifest z classpath
├── lib/                     ← biblioteki LWJGL dla Windows/Linux/Mac
├── src/craft3dgl/           ← kod źródłowy
├── build/craft3dgl/         ← skompilowane klasy
├── assets/sounds/           ← oryginalne eventy i warianty dźwięków Minecraft 1.12
├── META-INF/                ← META-INF
├── start_windows.bat        ← Windows launcher
├── start_linux_mac.sh       ← Linux/Mac launcher
├── URUCHOM_MNIE.txt         ← Instrukcja po polsku
└── README.md                ← Ten plik
```

## Tryby gry

- **Survival** - hp, głód, normalne kopanie z hardness blocków
- **Creative** - latanie (double-space), instant kopanie, dostęp do creative inv

## Świat

- Rozmiar: 1024 × 64 × 1024 bloków
- Biomy: Plains, Forest, Desert, Mountains, Ocean, Beach i River
- Wioski (~25% szans w komórkach 80×80)
- Rzeki, jeziora, symulacja wody
- Save w `saves/<nazwa>/world.dat`

## Modyfikacja kodu

Kod źródłowy jest w `src/craft3dgl/`. Po edycji rekompiluj:

**Windows:**
```cmd
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -cp "lib\*" -d build @sources.txt
jar cfm Craft3D.jar MANIFEST.MF -C build .
```

**Linux/Mac:**
```bash
find src -name '*.java' | sort > sources.txt
javac -encoding UTF-8 -cp "lib/*" -d build @sources.txt
jar cfm Craft3D.jar MANIFEST.MF -C build .
```
