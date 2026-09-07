package craft3dgl.input;

import java.util.HashMap;

/**
 * Tłumaczenia interfejsu (PL/EN). Mapa klucz -> [PL, EN].
 */
public final class Translations {
    private final HashMap<String, String[]> map = new HashMap<>();
    private String language = "pl";

    public Translations() {
        init();
    }

    public void setLanguage(String lang) {
        this.language = lang;
    }

    public String getLanguage() {
        return language;
    }

    /** Pobierz tłumaczenie. */
    public String tr(String key) {
        String[] arr = map.get(key);
        if (arr == null) return key;
        return "en".equals(language) ? arr[1] : arr[0];
    }

    public boolean isEnglish() {
        return "en".equals(language);
    }

    private void init() {
        map.put("menu.singleplayer", new String[]{"Tryb jednoosobowy", "Singleplayer"});
        map.put("menu.multiplayer", new String[]{"Tryb wieloosobowy", "Multiplayer"});
        map.put("menu.online", new String[]{"Minecraft Realms", "Minecraft Realms"});
        map.put("menu.newworld", new String[]{"Stwórz nowy świat", "Create New World"});
        map.put("menu.savecur", new String[]{"Zapisz aktualny świat", "Save Current World"});
        map.put("menu.options", new String[]{"Opcje...", "Options..."});
        map.put("menu.quit", new String[]{"Wyjdź z gry", "Quit Game"});
        map.put("menu.selectworld", new String[]{"Wybierz świat", "Select World"});
        map.put("menu.noworlds", new String[]{"Brak światów. Stwórz nowy!", "No worlds yet. Create one!"});
        map.put("menu.back", new String[]{"Wstecz", "Back"});
        map.put("pause.title", new String[]{"Menu Gry", "Game Menu"});
        map.put("pause.continue", new String[]{"Wróć do gry", "Back to Game"});
        map.put("pause.advancements", new String[]{"Postępy", "Advancements"});
        map.put("pause.statistics", new String[]{"Statystyki", "Statistics"});
        map.put("pause.settings", new String[]{"Opcje...", "Options..."});
        map.put("pause.openlan", new String[]{"Udostępnij w LAN", "Open to LAN"});
        map.put("pause.exit", new String[]{"Zapisz i wyjdź do menu", "Save and Quit to Title"});
        map.put("pause.esc", new String[]{"ESC - powrót do gry", "ESC - back to game"});
        map.put("settings.title", new String[]{"Ustawienia", "Settings"});
        map.put("settings.sounds", new String[]{"Dźwięki", "Sounds"});
        map.put("settings.language", new String[]{"Język", "Language"});
        map.put("settings.done", new String[]{"Gotowe", "Done"});
        map.put("settings.master", new String[]{"Głośność ogólna", "Master Volume"});
        map.put("settings.music", new String[]{"Muzyka", "Music"});
        map.put("settings.blocks", new String[]{"Bloki", "Blocks"});
        map.put("settings.hostile", new String[]{"Wrogie", "Hostile"});
        map.put("settings.animals", new String[]{"Zwierzęta", "Animals"});
        map.put("settings.players", new String[]{"Gracze", "Players"});
        map.put("settings.ambient", new String[]{"Otoczenie", "Ambient"});
        map.put("settings.ui", new String[]{"UI / Kliknięcia", "UI / Clicks"});
        map.put("options.title", new String[]{"Opcje", "Options"});
        map.put("options.sounds", new String[]{"Muzyka i dźwięk...", "Music & Sound Settings..."});
        map.put("options.language", new String[]{"Język...", "Language..."});
        map.put("options.video", new String[]{"Grafika...", "Video Settings..."});
        map.put("options.controls", new String[]{"Sterowanie...", "Controls..."});
        map.put("options.resourcepack", new String[]{"Pakiety zasobów...", "Resource Packs..."});
        map.put("options.snooper.view", new String[]{"Ustawienia Snoopera", "Snooper Settings"});
        map.put("options.sounds.title", new String[]{"Muzyka i dźwięk", "Music & Sound Options"});
        map.put("options.off", new String[]{"WYŁ", "OFF"});
        map.put("creative.title", new String[]{"Ekwipunek trybu kreatywnego", "Creative Inventory"});
        map.put("creative.trash", new String[]{"Wyrzuć tu (czyści)", "Trash here (clears)"});
        map.put("creative.clearall", new String[]{"Wyczyść cały ekwipunek", "Clear Inventory"});
        map.put("creative.all", new String[]{"Wszystko", "All"});
        map.put("creative.blocks", new String[]{"Bloki", "Blocks"});
        map.put("creative.tools", new String[]{"Narzędzia", "Tools"});
        map.put("creative.food", new String[]{"Jedzenie", "Food"});
        map.put("creative.misc", new String[]{"Inne", "Miscellaneous"});
        map.put("creative.search", new String[]{"Szukaj...", "Search..."});
        map.put("creative.trash.short", new String[]{"Kosz", "Trash"});
        map.put("inv.title", new String[]{"Ekwipunek", "Inventory"});
        map.put("crafting.title", new String[]{"Stół rzemieślniczy", "Crafting Table"});
        map.put("chest.title", new String[]{"Skrzynia", "Chest"});
        map.put("chest.large.title", new String[]{"Duża skrzynia", "Large Chest"});
        map.put("section.craft", new String[]{"Konstruowanie", "Crafting"});
        map.put("section.inventory", new String[]{"Ekwipunek", "Inventory"});
        map.put("section.hotbar", new String[]{"Hotbar", "Hotbar"});
        map.put("section.armor", new String[]{"Zbroja", "Armor"});
        map.put("section.off", new String[]{"Off", "Off"});
        map.put("section.items", new String[]{"Itemy", "Items"});
        map.put("section.chest", new String[]{"Skrzynka", "Chest"});
        map.put("death.title", new String[]{"NIE ŻYJESZ!", "YOU DIED!"});
        map.put("death.respawn", new String[]{"Odrodzenie", "Respawn"});
        map.put("death.titlescr", new String[]{"Menu główne", "Title Screen"});
        map.put("death.dropped", new String[]{"Twoje przedmioty wypadły na ziemię", "Your items dropped on the ground"});
    }
}
