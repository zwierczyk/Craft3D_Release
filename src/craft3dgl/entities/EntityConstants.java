package craft3dgl.entities;

/**
 * Stałe rozmiarów i parametrów entity (zwierzęta, villagerzy).
 */
public final class EntityConstants {
    private EntityConstants() {}

    // === Zwierzęta ===
    public static final double ANIMAL_RADIUS = 0.40;
    public static final double ANIMAL_HEIGHT = 1.30;

    public static final int COW_HEALTH = 12;
    public static final int SHEEP_HEALTH = 8;
    public static final int PIG_HEALTH = 10;

    public static final double COW_SPEED = 0.50;
    public static final double SHEEP_SPEED = 0.55;
    public static final double PIG_SPEED = 0.60;

    public static final double COW_PANIC_SPEED = 6.0;
    public static final double SHEEP_PANIC_SPEED = 6.6;
    public static final double PIG_PANIC_SPEED = 7.2;

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
