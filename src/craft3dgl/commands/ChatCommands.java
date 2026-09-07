package craft3dgl.commands;

import craft3dgl.items.ItemNames;

/**
 * Parser komend chatu (/gamemode, /fly, /tp, /give, /effect, ...).
 * Wymaga callbacka do faktycznych akcji w grze.
 */
public final class ChatCommands {

    public interface CommandContext {
        void addChatMessage(String msg);
        void setGameMode(int mode);
        void toggleFly();
        boolean isCreative();
        void teleport(double x, double y, double z);
        boolean giveItem(int id, int count);
        int getFps();
        void rebuildVillages();
        /** Aplikuje efekt (np. "night_vision") na X sekund. Zwraca true jesli typ znany. */
        boolean applyEffect(String type, int seconds);
        /** Czysci wszystkie efekty gracza. */
        void clearEffects();
        default boolean setTime(String value) { return false; }
        default boolean setWeather(String value) { return false; }
    }

    public static final int GAMEMODE_SURVIVAL = 0;
    public static final int GAMEMODE_CREATIVE = 1;

    private ChatCommands() {}

    private static final String[] COMMANDS = {
            "gamemode", "fly", "tp", "give", "effect", "time", "weather",
            "fps", "rebuild", "rebuildvillages", "help"
    };
    private static final String[] GIVE_ITEMS = {
            "grass", "dirt", "stone", "wood", "leaves", "sand", "planks", "glass",
            "glass_pane",
            "crafting_table", "door", "chest", "water", "stick",
            "wood_pickaxe", "stone_pickaxe", "wood_axe", "stone_axe",
            "wood_shovel", "stone_shovel", "wood_sword", "stone_sword",
            "wood_hoe", "stone_hoe", "pork", "beef", "mutton", "emerald",
            "bread", "seeds", "wheat", "tall_grass", "farmland"
    };

    /**
     * Client-side TabCompleter equivalent. Returned strings replace the whole
     * edit field, which makes repeated Tab cycle through valid command options.
     */
    public static java.util.List<String> complete(String input) {
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        if (input == null || !input.startsWith("/")) return out;
        String body = input.substring(1);
        int lastSpace = body.lastIndexOf(' ');
        if (lastSpace < 0) {
            addMatches(out, "/", body, COMMANDS, true);
            return out;
        }
        String[] parts = body.substring(0, lastSpace + 1).trim().split("\\s+");
        String command = parts.length == 0 ? "" : parts[0].toLowerCase();
        String prefix = body.substring(lastSpace + 1).toLowerCase();
        String base = "/" + body.substring(0, lastSpace + 1);
        if (command.equals("gamemode") || command.equals("gm")) {
            addMatches(out, base, prefix, new String[]{"survival", "creative"}, false);
        } else if (command.equals("weather")) {
            addMatches(out, base, prefix, new String[]{"clear", "rain", "thunder"}, false);
        } else if (command.equals("effect")) {
            addMatches(out, base, prefix, new String[]{"night_vision", "clear"}, false);
        } else if (command.equals("give")) {
            addMatches(out, base, prefix, GIVE_ITEMS, false);
        } else if (command.equals("time")) {
            if (parts.length <= 1) addMatches(out, base, prefix, new String[]{"set"}, true);
            else if (parts.length == 2 && parts[1].equalsIgnoreCase("set")) {
                addMatches(out, base, prefix, new String[]{"day", "night"}, false);
            }
        }
        return out;
    }

    private static void addMatches(java.util.List<String> out, String base, String prefix,
                                   String[] values, boolean appendSpace) {
        for (String value : values) {
            if (value.startsWith(prefix.toLowerCase())) {
                out.add(base + value + (appendSpace ? " " : ""));
            }
        }
    }

    /** Wykonaj komende, np. "/give wheat 10". */
    public static void handle(String cmd, CommandContext ctx) {
        String body = cmd.startsWith("/") ? cmd.substring(1).trim() : cmd.trim();
        if (body.isEmpty()) { ctx.addChatMessage("[Pusta komenda]"); return; }
        String[] parts = body.split("\\s+");
        String name = parts[0].toLowerCase();
        switch (name) {
            case "gamemode": case "gm":
                if (parts.length < 2) { ctx.addChatMessage("Uzycie: /gamemode <creative|survival|c|s|0|1>"); return; }
                String arg = parts[1].toLowerCase();
                if (arg.equals("creative") || arg.equals("c") || arg.equals("1")) {
                    ctx.setGameMode(GAMEMODE_CREATIVE);
                    ctx.addChatMessage("Tryb zmieniony na Creative");
                } else if (arg.equals("survival") || arg.equals("s") || arg.equals("0")) {
                    ctx.setGameMode(GAMEMODE_SURVIVAL);
                    ctx.addChatMessage("Tryb zmieniony na Survival");
                } else {
                    ctx.addChatMessage("Nieznany tryb: " + arg);
                }
                break;
            case "fly":
                if (ctx.isCreative()) {
                    ctx.toggleFly();
                } else {
                    ctx.addChatMessage("Latanie tylko w creative");
                }
                break;
            case "tp":
                if (parts.length < 4) { ctx.addChatMessage("Uzycie: /tp <x> <y> <z>"); return; }
                try {
                    double tx = Double.parseDouble(parts[1]);
                    double ty = Double.parseDouble(parts[2]);
                    double tz = Double.parseDouble(parts[3]);
                    ctx.teleport(tx, ty, tz);
                    ctx.addChatMessage("Teleport do " + tx + " " + ty + " " + tz);
                } catch (Exception e) { ctx.addChatMessage("Bledne wspolrzedne"); }
                break;
            case "give":
                if (parts.length < 2) { ctx.addChatMessage("Uzycie: /give <id_lub_nazwa> [ilosc]"); return; }
                int gid = ItemNames.parseItemName(parts[1]);
                if (gid <= 0) { ctx.addChatMessage("Nieznany item: " + parts[1]); return; }
                int gcount = 1;
                if (parts.length >= 3) {
                    try { gcount = Integer.parseInt(parts[2]); } catch (Exception ignored) {}
                }
                gcount = Math.max(1, Math.min(gcount, 64));
                if (ctx.giveItem(gid, gcount)) {
                    ctx.addChatMessage("Otrzymano " + ItemNames.itemName(gid, "pl") + " x" + gcount);
                } else {
                    ctx.addChatMessage("Brak miejsca w inventory");
                }
                break;
            case "effect":
                if (parts.length < 2) {
                    ctx.addChatMessage("Uzycie: /effect <night_vision|clear> [sekundy]");
                    return;
                }
                String etype = parts[1].toLowerCase();
                if (etype.equals("clear") || etype.equals("off") || etype.equals("remove")) {
                    ctx.clearEffects();
                    ctx.addChatMessage("Usunieto wszystkie efekty");
                    return;
                }
                int esecs = 30;
                if (parts.length >= 3) {
                    try { esecs = Integer.parseInt(parts[2]); } catch (Exception ignored) {}
                }
                esecs = Math.max(1, Math.min(esecs, 99999));
                if (ctx.applyEffect(etype, esecs)) {
                    ctx.addChatMessage("Efekt " + etype + " na " + esecs + "s");
                } else {
                    ctx.addChatMessage("Nieznany efekt: " + etype + " (dostepny: night_vision)");
                }
                break;
            case "time":
                if (parts.length >= 3 && parts[1].equalsIgnoreCase("set")) {
                    if (ctx.setTime(parts[2])) ctx.addChatMessage("Ustawiono czas: " + parts[2]);
                    else ctx.addChatMessage("Uzycie: /time set <day|night|liczba>");
                } else {
                    ctx.addChatMessage("Uzycie: /time set <day|night|liczba>");
                }
                break;
            case "weather":
                if (parts.length >= 2 && ctx.setWeather(parts[1])) {
                    ctx.addChatMessage("Ustawiono pogode: " + parts[1].toLowerCase());
                } else {
                    ctx.addChatMessage("Uzycie: /weather <clear|rain|thunder>");
                }
                break;
            case "fps":
                ctx.addChatMessage("FPS: " + ctx.getFps());
                break;
            case "rebuild": case "rebuildvillages":
                ctx.rebuildVillages();
                break;
            case "help":
                ctx.addChatMessage("Komendy: /gamemode, /fly, /tp, /give, /effect, /time set, /weather, /fps, /rebuild, /help");
                break;
            default:
                ctx.addChatMessage("Nieznana komenda: /" + name);
        }
    }
}
