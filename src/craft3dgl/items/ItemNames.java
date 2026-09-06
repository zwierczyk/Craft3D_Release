package craft3dgl.items;

import static craft3dgl.world.WorldConstants.*;

/**
 * Lokalizowane nazwy itemów + parsowanie nazw z komend chatu (/give NAZWA).
 */
public final class ItemNames {
    private ItemNames() {}

    /** Nazwa itemu w jezyku PL lub EN. */
    public static String itemName(int id, String language) {
        boolean en = "en".equals(language);
        switch (id) {
            case GRASS: return en ? "Grass" : "Trawa";
            case DIRT: return en ? "Dirt" : "Ziemia";
            case STONE: return en ? "Stone" : "Kamien";
            case WOOD: return en ? "Wood" : "Drewno";
            case LEAVES: return en ? "Leaves" : "Liscie";
            case SAND: return en ? "Sand" : "Piasek";
            case PLANKS: return en ? "Planks" : "Deski";
            case CRAFTING_TABLE: return en ? "Crafting Table" : "Stol craftingowy";
            case DOOR_BOTTOM: case DOOR_TOP: return en ? "Oak Door" : "Debowe drzwi";
            case CHEST: return en ? "Chest" : "Skrzynka";
            case WATER: return en ? "Water" : "Woda";
            case ITEM_STICK: return en ? "Sticks" : "Patyki";
            case ITEM_WOOD_PICKAXE: return en ? "Wood Pickaxe" : "Drewniany kilof";
            case ITEM_STONE_PICKAXE: return en ? "Stone Pickaxe" : "Kamienny kilof";
            case ITEM_WOOD_AXE: return en ? "Wood Axe" : "Drewniana siekiera";
            case ITEM_STONE_AXE: return en ? "Stone Axe" : "Kamienna siekiera";
            case ITEM_WOOD_SHOVEL: return en ? "Wood Shovel" : "Drewniana lopata";
            case ITEM_STONE_SHOVEL: return en ? "Stone Shovel" : "Kamienna lopata";
            case ITEM_WOOD_SWORD: return en ? "Wood Sword" : "Drewniany miecz";
            case ITEM_STONE_SWORD: return en ? "Stone Sword" : "Kamienny miecz";
            case ITEM_PORK: return en ? "Pork" : "Wieprzowina";
            case ITEM_BEEF: return en ? "Beef" : "Wolowina";
            case ITEM_MUTTON: return en ? "Mutton" : "Baranina";
            case ITEM_EMERALD: return en ? "Emerald" : "Szmaragd";
            case ITEM_BREAD: return en ? "Bread" : "Chleb";
            case ITEM_SEEDS: return en ? "Wheat Seeds" : "Nasiona pszenicy";
            case ITEM_WHEAT: return en ? "Wheat" : "Pszenica";
            case ITEM_WOOD_HOE: return en ? "Wood Hoe" : "Drewniana motyka";
            case ITEM_STONE_HOE: return en ? "Stone Hoe" : "Kamienna motyka";
            case TALL_GRASS: return en ? "Tall Grass" : "Trawka";
            case FARMLAND: return en ? "Farmland" : "Pole uprawne";
            case WHEAT_0: case WHEAT_1: case WHEAT_2: case WHEAT_3:
                return en ? "Wheat Crop" : "Pszenica (rosnie)";
            default: return (en ? "Item " : "Przedmiot ") + id;
        }
    }

    /** Parsuje nazwe z komendy (np. "/give seeds 5"). Zwraca -1 gdy nieznana. */
    public static int parseItemName(String name) {
        try { return Integer.parseInt(name); } catch (Exception ignored) {}
        switch (name.toLowerCase()) {
            case "grass": return GRASS;
            case "dirt": return DIRT;
            case "stone": return STONE;
            case "wood": case "log": return WOOD;
            case "leaves": return LEAVES;
            case "sand": return SAND;
            case "planks": return PLANKS;
            case "crafting_table": case "table": return CRAFTING_TABLE;
            case "door": case "oak_door": case "wooden_door": case "drzwi": return DOOR_BOTTOM;
            case "chest": return CHEST;
            case "water": return WATER;
            case "stick": return ITEM_STICK;
            case "wood_pickaxe": case "wpick": return ITEM_WOOD_PICKAXE;
            case "stone_pickaxe": case "spick": return ITEM_STONE_PICKAXE;
            case "wood_axe": case "waxe": return ITEM_WOOD_AXE;
            case "stone_axe": case "saxe": return ITEM_STONE_AXE;
            case "wood_shovel": case "wshovel": return ITEM_WOOD_SHOVEL;
            case "stone_shovel": case "sshovel": return ITEM_STONE_SHOVEL;
            case "wood_sword": case "wsword": return ITEM_WOOD_SWORD;
            case "stone_sword": case "ssword": return ITEM_STONE_SWORD;
            case "pork": return ITEM_PORK;
            case "beef": return ITEM_BEEF;
            case "mutton": return ITEM_MUTTON;
            case "emerald": case "szmaragd": return ITEM_EMERALD;
            case "bread": case "chleb": return ITEM_BREAD;
            case "seeds": case "nasiona": case "nasionka": return ITEM_SEEDS;
            case "wheat": case "pszenica": return ITEM_WHEAT;
            case "wood_hoe": case "whoe": case "motyka": case "drewniana_motyka": return ITEM_WOOD_HOE;
            case "stone_hoe": case "shoe": case "kamienna_motyka": return ITEM_STONE_HOE;
            case "tall_grass": case "trawka": return TALL_GRASS;
            case "farmland": case "pole": return FARMLAND;
            default: return -1;
        }
    }
}
