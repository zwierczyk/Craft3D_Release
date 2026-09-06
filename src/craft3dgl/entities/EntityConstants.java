package craft3dgl.entities;

/**
 * Stałe rozmiarów i parametrów entity (zwierzęta, villagerzy).
 */
public final class EntityConstants {
    private EntityConstants() {}

    // === Zwierzęta (EntityPig/Cow/Sheep z MCP 9.40) ===
    public static final double ANIMAL_RADIUS = 0.45; // wszystkie maja width=0.9
    public static final double PIG_HEIGHT = 0.90;
    public static final double COW_HEIGHT = 1.40;
    public static final double SHEEP_HEIGHT = 1.30;

    public static final int COW_HEALTH = 10;
    public static final int SHEEP_HEALTH = 8;
    public static final int PIG_HEALTH = 10;

    public static double animalHeight(int type) {
        if (type == craft3dgl.AnimalGL.PIG) return PIG_HEIGHT;
        if (type == craft3dgl.AnimalGL.COW) return COW_HEIGHT;
        return SHEEP_HEIGHT;
    }

    public static int animalMaxHealth(int type) {
        if (type == craft3dgl.AnimalGL.COW) return COW_HEALTH;
        if (type == craft3dgl.AnimalGL.SHEEP) return SHEEP_HEALTH;
        return PIG_HEALTH;
    }

    public static double animalEyeHeight(int type) {
        if (type == craft3dgl.AnimalGL.COW) return 1.30;
        if (type == craft3dgl.AnimalGL.SHEEP) return SHEEP_HEIGHT * 0.95;
        return PIG_HEIGHT * 0.85;
    }

    public static final double COW_SPEED = 0.50;
    public static final double SHEEP_SPEED = 0.55;
    public static final double PIG_SPEED = 0.60;

    // EntityAIPanic speed: cow=2.0, pig/sheep=1.25 wzgledem zwyklego ruchu.
    public static final double COW_PANIC_SPEED = COW_SPEED * 2.0;
    public static final double SHEEP_PANIC_SPEED = SHEEP_SPEED * 1.25;
    public static final double PIG_PANIC_SPEED = PIG_SPEED * 1.25;

    public static double animalSpeed(int type) {
        if (type == craft3dgl.AnimalGL.COW) return COW_SPEED;
        if (type == craft3dgl.AnimalGL.SHEEP) return SHEEP_SPEED;
        return PIG_SPEED;
    }

    public static double animalPanicSpeed(int type) {
        if (type == craft3dgl.AnimalGL.COW) return COW_PANIC_SPEED;
        if (type == craft3dgl.AnimalGL.SHEEP) return SHEEP_PANIC_SPEED;
        return PIG_PANIC_SPEED;
    }

    public static final double ANIMAL_PANIC_TIME = 5.0;
    public static final double ANIMAL_JUMP_VEL = 7.0;
    public static final double ANIMAL_GRAVITY = 22.0;
    public static final double ANIMAL_GRAVITY_WATER = 4.0;

    // === Villagerzy ===
    public static final double VILLAGER_RADIUS = 0.34;
    public static final double VILLAGER_HEIGHT = 1.80;
    public static final int VILLAGER_HEALTH = 20;
    public static final double VILLAGER_SPEED = 0.55;
    public static final double VILLAGER_HOME_RANGE = 25.0; // wracaj jak dalej
    public static final double VILLAGER_JUMP_VEL = 8.5;
}
