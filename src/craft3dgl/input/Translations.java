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
        map.put("menu.singleplayer", new String[]{"Singleplayer / Swiaty", "Singleplayer / Worlds"});
        map.put("menu.newworld", new String[]{"Stworz Nowy Swiat", "Create New World"});
        map.put("menu.savecur", new String[]{"Zapisz Aktualny Swiat", "Save Current World"});
        map.put("menu.quit", new String[]{"Wyjdz z gry", "Quit Game"});
        map.put("menu.selectworld", new String[]{"Wybierz Swiat", "Select World"});
        map.put("menu.noworlds", new String[]{"Brak swiatow. Stworz nowy!", "No worlds yet. Create one!"});
        map.put("menu.back", new String[]{"Wstecz", "Back"});
        map.put("pause.title", new String[]{"Menu Gry", "Game Menu"});
        map.put("pause.continue", new String[]{"Graj dalej", "Continue"});
        map.put("pause.settings", new String[]{"Ustawienia", "Settings"});
        map.put("pause.exit", new String[]{"Wyjdz do menu glownego", "Exit to Main Menu"});
        map.put("pause.esc", new String[]{"ESC - powrot do gry", "ESC - back to game"});
        map.put("settings.title", new String[]{"Ustawienia", "Settings"});
        map.put("settings.sounds", new String[]{"Dzwieki", "Sounds"});
        map.put("settings.language", new String[]{"Jezyk", "Language"});
        map.put("settings.done", new String[]{"Gotowe", "Done"});
        map.put("settings.master", new String[]{"Master", "Master"});
        map.put("settings.music", new String[]{"Muzyka", "Music"});
        map.put("settings.blocks", new String[]{"Bloki", "Blocks"});
        map.put("settings.hostile", new String[]{"Wrogie", "Hostile"});
        map.put("settings.animals", new String[]{"Zwierzeta", "Animals"});
        map.put("settings.players", new String[]{"Gracze", "Players"});
        map.put("settings.ambient", new String[]{"Otoczenie", "Ambient"});
        map.put("settings.ui", new String[]{"UI / Klikniecia", "UI / Clicks"});
        map.put("creative.title", new String[]{"Creative Inventory", "Creative Inventory"});
        map.put("creative.trash", new String[]{"Wyrzuc tu (czysci)", "Trash here (clears)"});
        map.put("creative.clearall", new String[]{"Wyczysc caly ekwipunek", "Clear Inventory"});
        map.put("creative.all", new String[]{"Wszystko", "All"});
        map.put("creative.blocks", new String[]{"Bloki", "Blocks"});
        map.put("creative.tools", new String[]{"Narzedzia", "Tools"});
        map.put("creative.food", new String[]{"Jedzenie", "Food"});
        map.put("creative.search", new String[]{"Szukaj...", "Search..."});
        map.put("creative.trash.short", new String[]{"Kosz", "Trash"});
        map.put("inv.title", new String[]{"Ekwipunek", "Inventory"});
        map.put("crafting.title", new String[]{"Stol Rzemieslniczy", "Crafting Table"});
        map.put("chest.title", new String[]{"Skrzynia", "Chest"});
        map.put("section.craft", new String[]{"Konstruowanie", "Crafting"});
        map.put("section.inventory", new String[]{"Ekwipunek", "Inventory"});
        map.put("section.hotbar", new String[]{"Hotbar", "Hotbar"});
        map.put("section.armor", new String[]{"Zbroja", "Armor"});
        map.put("section.off", new String[]{"Off", "Off"});
        map.put("section.items", new String[]{"Itemy", "Items"});
        map.put("section.chest", new String[]{"Skrzynka", "Chest"});
        map.put("death.title", new String[]{"ZGINALES!", "YOU DIED!"});
        map.put("death.respawn", new String[]{"Respawn", "Respawn"});
        map.put("death.titlescr", new String[]{"Menu glowne", "Title Screen"});
        map.put("death.dropped", new String[]{"Twoje itemy wypadly na ziemie", "Your items dropped on the ground"});
    }
}
