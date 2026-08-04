package craft3dgl.combat;

import static craft3dgl.world.WorldConstants.*;

/**
 * System obrazen i obliczanie damage z roznych broni / akcji.
 */
public final class DamageSystem {
    private DamageSystem() {}

    /** Damage zadawany zwierzeciu z danego itemu (miecz, kilof, etc.) */
    public static int meleeDamage(int weaponId) {
        if (weaponId == ITEM_STONE_SWORD) return 8;
        if (weaponId == ITEM_WOOD_SWORD) return 6;
        if (weaponId == ITEM_STONE_AXE || weaponId == ITEM_STONE_PICKAXE) return 5;
        if (weaponId == ITEM_WOOD_AXE || weaponId == ITEM_WOOD_PICKAXE) return 4;
        return 3; // pusta reka
    }

    /** Damage zadawany villagerowi (jak dla zwierzecia). */
    public static int villagerDamage(int weaponId) {
        if (weaponId == ITEM_STONE_SWORD) return 8;
        if (weaponId == ITEM_WOOD_SWORD) return 6;
        return 3;
    }

    /** Ile dany item przywraca głodu. */
    public static int foodValue(int foodId) {
        if (foodId == ITEM_BEEF) return 8;
        if (foodId == ITEM_MUTTON) return 6;
        if (foodId == ITEM_BREAD) return 5;
        if (foodId == ITEM_PORK) return 6;
        return 0;
    }

    /** Czy item jest jedzeniem. */
    public static boolean isFood(int id) {
        return id == ITEM_PORK || id == ITEM_BEEF || id == ITEM_MUTTON || id == ITEM_BREAD;
    }
}
