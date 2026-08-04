package craft3dgl.entities;

/**
 * Statystyki gracza: HP, glod, timery regeneracji.
 * Logika tickow trzymana tu, ale damage callbacki nadal w MinecraftGL.
 */
public final class PlayerStats {
    public int health;
    public int maxHealth = 20;
    public int hunger = 20;
    public double hungerTimer = 0;
    public double regenTimer = 0;
    public double starveTimer = 0;

    public static final int MAX_HUNGER = 20;
    public static final double HUNGER_DRAIN_SECONDS = 18.0;
    public static final double REGEN_INTERVAL = 4.0;
    public static final int REGEN_HUNGER_THRESHOLD = 18;
    public static final double STARVE_DAMAGE_INTERVAL = 5.0;

    public PlayerStats() {
        this.health = maxHealth;
    }

    public void reset() {
        health = maxHealth;
        hunger = 20;
        hungerTimer = regenTimer = starveTimer = 0;
    }

    /** Tick statystyk - zwraca > 0 jezeli gracz powinien dostac damage od glodu. */
    public int tick(double dt, boolean moving, boolean creative) {
        if (creative) {
            health = maxHealth;
            hunger = MAX_HUNGER;
            hungerTimer = regenTimer = starveTimer = 0;
            return 0;
        }
        hungerTimer += dt * (moving ? 1.0 : 0.22);
        if (hungerTimer >= HUNGER_DRAIN_SECONDS) {
            hungerTimer = 0;
            if (hunger > 0) hunger--;
        }
        if (hunger >= REGEN_HUNGER_THRESHOLD && health < maxHealth) {
            regenTimer += dt;
            if (regenTimer >= REGEN_INTERVAL) {
                regenTimer = 0;
                health++;
            }
        } else {
            regenTimer = 0;
        }
        if (hunger <= 0) {
            starveTimer += dt;
            if (starveTimer >= STARVE_DAMAGE_INTERVAL) {
                starveTimer = 0;
                return 1;  // damage 1 HP od glodu
            }
        } else {
            starveTimer = 0;
        }
        return 0;
    }
}
