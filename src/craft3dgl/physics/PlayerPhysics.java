package craft3dgl.physics;

/**
 * Stałe i pomocnicze fizyki gracza.
 */
public final class PlayerPhysics {
    private PlayerPhysics() {}

    public static final double PLAYER_RADIUS = 0.28;
    public static final double PLAYER_HEIGHT = 1.78;
    public static final double EYE_HEIGHT = 1.55;

    // Grawitacja
    public static final double GRAVITY = 18.0;             // m/s² na powietrzu
    public static final double GRAVITY_WATER = 4.0;        // m/s² w wodzie
    public static final double TERMINAL_VELOCITY = -24.0;  // max prędkość spadania
    public static final double TERMINAL_WATER = -3.2;

    // Skok
    public static final double JUMP_VELOCITY = 7.2;
    public static final double JUMP_VELOCITY_WATER = 3.2;

    // Latanie (creative)
    public static final double FLY_SPEED = 12.0;
    public static final double FLY_SPRINT_SPEED = 22.0;
    public static final double FLY_CLIMB_SPEED = 14.0;
    public static final double FLY_CLIMB_SPRINT = 28.0;

    // Chodzenie
    public static final double WALK_SPEED = 5.0;
    public static final double SPRINT_SPEED = 8.0;
    public static final double WATER_SPEED_MULT = 0.50;

    // Damage przy upadku
    public static final double FALL_DAMAGE_THRESHOLD = -13.0;
    public static final double FALL_DAMAGE_MULT = 1.15;

    /** Czy gracz powinien dostać damage od upadku? Zwraca ilość damage (0 = bez). */
    public static int calcFallDamage(double velY, boolean inWater) {
        if (inWater) return 0;
        if (velY > FALL_DAMAGE_THRESHOLD) return 0;
        return (int) Math.ceil((-velY - 12.0) * FALL_DAMAGE_MULT);
    }
}
