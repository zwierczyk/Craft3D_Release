package craft3dgl.audio;

/**
 * Kategorie głośności dźwięku (jak w MC settings: Music, Blocks, Hostile, Animals, Players, Ambient, UI).
 */
public final class AudioCategories {
    private AudioCategories() {}

    public static final String MUSIC = "music";
    public static final String BLOCKS = "blocks";
    public static final String HOSTILE = "hostile";
    public static final String ANIMALS = "animals";
    public static final String PLAYERS = "players";
    public static final String AMBIENT = "ambient";
    public static final String UI = "ui";

    /** Wszystkie kategorie w kolejnosci wyswietlania w UI ustawien. */
    public static final String[] ALL = {
        MUSIC, BLOCKS, HOSTILE, ANIMALS, PLAYERS, AMBIENT, UI
    };

    /** Tlumaczone labelki - klucz w Translations. */
    public static final String[] TRANSLATION_KEYS = {
        "settings.music", "settings.blocks", "settings.hostile",
        "settings.animals", "settings.players", "settings.ambient", "settings.ui"
    };
}
