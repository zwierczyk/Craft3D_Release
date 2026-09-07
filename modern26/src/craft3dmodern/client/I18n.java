package craft3dmodern.client;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import craft3dmodern.render.Texture;
import craft3dmodern.util.Json;

/**
 * Tlumaczenia: en_us.json ladujemy z prawdziwych assetow 26.2; poniewaz
 * klient zawiera tylko en_us, polskie napisy dla uzywanych kluczy trzymamy
 * lokalnie (jak w starszych wersjach MC z pl_PL.lang).
 */
public final class I18n {
    public enum Lang { EN, PL }

    private static final Map<String, String> EN = new HashMap<String, String>();
    private static final Map<String, String> PL = new HashMap<String, String>();
    private static Lang current = Lang.EN;
    private static boolean loaded = false;

    private I18n() {}

    public static void init() throws IOException {
        File en = new File(new File(Texture.assetRoot(), "minecraft/lang"), "en_us.json");
        if (en.isFile()) {
            Object root = Json.parseFile(en.toPath());
            for (Map.Entry<String, Object> e : Json.asObject(root).entrySet()) {
                if (e.getValue() instanceof String) EN.put(e.getKey(), (String) e.getValue());
            }
        }
        // Polski - recznie, dla kluczy ktore wyswietlamy (odpowiedniki pl_PL).
        putPl("menu.singleplayer", "Tryb jednoosobowy");
        putPl("menu.multiplayer", "Tryb wieloosobowy");
        putPl("menu.online", "Minecraft Realms");
        putPl("menu.options", "Opcje...");
        putPl("menu.quit", "Wyjdź z gry");
        putPl("selectWorld.title", "Wybierz świat");
        putPl("selectWorld.empty", "Brak światów. Stwórz nowy!");
        putPl("selectWorld.create", "Stwórz nowy świat");
        putPl("selectWorld.select", "Wybierz świat");
        putPl("selectWorld.edit", "Edytuj");
        putPl("selectWorld.delete", "Usuń");
        putPl("selectWorld.recreate", "Odtwórz");
        putPl("selectWorld.back", "Wstecz");
        putPl("gui.done", "Gotowe");
        putPl("gui.cancel", "Anuluj");
        putPl("gui.back", "Wstecz");
        putPl("options.title", "Opcje");
        putPl("options.language", "Język...");
        putPl("options.sounds", "Muzyka i dźwięk...");
        putPl("options.video", "Grafika...");
        putPl("options.controls", "Sterowanie...");
        putPl("options.language.title", "Język");
        putPl("options.languageWarning", "Tłumaczenia nie są kompletne");
        putPl("options.off", "WYŁ");
        putPl("language.name", "Polski");
        putPl("menu.game", "Menu Gry");
        putPl("menu.returnToGame", "Wróć do gry");
        putPl("menu.returnToMenu", "Zapisz i wyjdź do menu");
        putPl("menu.copyright", "Prawa autorskie: Mojang AB");
        putPl("menu.splash", "");
        loaded = true;
    }

    private static void putPl(String k, String v) {
        PL.put(k, v);
    }

    public static void setLang(Lang l) {
        current = l;
    }

    public static Lang lang() {
        return current;
    }

    public static boolean isPl() {
        return current == Lang.PL;
    }

    /** Tlumaczenie klucza (PL -> wlasna mapa z fallbackiem do en_us). */
    public static String get(String key) {
        String v = current == Lang.PL ? PL.get(key) : null;
        if (v == null) v = EN.get(key);
        return v == null ? key : v;
    }
}
