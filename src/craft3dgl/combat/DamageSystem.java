package craft3dgl.combat;

import static craft3dgl.world.WorldConstants.*;

/** Minecraft 1.12 melee attributes, cooldown scaling, hurt resistance and knockback. */
public final class DamageSystem {
    private DamageSystem() {}

    public static final double MAX_HURT_RESISTANT_SECONDS = 1.0; // 20 ticks
    public static final double HURT_FLASH_SECONDS = 0.5;          // 10 ticks

    /** Player base damage (1) plus the held item's 1.12 attribute modifier. */
    public static float meleeDamage(int item) {
        if (item == ITEM_WOOD_SWORD) return 4.0f;
        if (item == ITEM_STONE_SWORD) return 5.0f;
        if (item == ITEM_WOOD_AXE) return 7.0f;
        if (item == ITEM_STONE_AXE) return 9.0f;
        if (item == ITEM_WOOD_PICKAXE) return 2.0f;
        if (item == ITEM_STONE_PICKAXE) return 3.0f;
        if (item == ITEM_WOOD_SHOVEL) return 2.5f;
        if (item == ITEM_STONE_SHOVEL) return 3.5f;
        // Hoes and all non-tools use the player's generic attack damage.
        return 1.0f;
    }

    /** Generic attack speed 4.0 plus the item's MCP attribute modifier. */
    public static float attackSpeed(int item) {
        if (item == ITEM_WOOD_SWORD || item == ITEM_STONE_SWORD) return 1.6f;
        if (item == ITEM_WOOD_AXE || item == ITEM_STONE_AXE) return 0.8f;
        if (item == ITEM_WOOD_PICKAXE || item == ITEM_STONE_PICKAXE) return 1.2f;
        if (item == ITEM_WOOD_SHOVEL || item == ITEM_STONE_SHOVEL) return 1.0f;
        if (item == ITEM_WOOD_HOE) return 1.0f;
        if (item == ITEM_STONE_HOE) return 2.0f;
        return 4.0f;
    }

    public static float cooldownPeriodTicks(int item) {
        return 20.0f / attackSpeed(item);
    }

    /** EntityPlayer.getCooledAttackStrength(adjustTicks), clamped to [0,1]. */
    public static float cooledAttackStrength(double ticksSinceLastSwing, int item, float adjustTicks) {
        float strength = (float)((ticksSinceLastSwing + adjustTicks) / cooldownPeriodTicks(item));
        return Math.max(0.0f, Math.min(1.0f, strength));
    }

    /** EntityPlayer's 1.12 quadratic cooldown damage curve. */
    public static float scaledDamage(int item, float cooledStrength) {
        float base = meleeDamage(item);
        return base * (0.2f + cooledStrength * cooledStrength * 0.8f);
    }

    public static boolean isSword(int item) {
        return item == ITEM_WOOD_SWORD || item == ITEM_STONE_SWORD;
    }

    public static final class AttackResult {
        public final float cooledStrength;
        public final float damage;
        public final boolean strong;
        public final boolean critical;
        public final boolean sprintKnockback;
        public final boolean sweeping;

        AttackResult(float cooledStrength, float damage, boolean strong, boolean critical,
                     boolean sprintKnockback, boolean sweeping) {
            this.cooledStrength = cooledStrength;
            this.damage = damage;
            this.strong = strong;
            this.critical = critical;
            this.sprintKnockback = sprintKnockback;
            this.sweeping = sweeping;
        }
    }

    /** The enchantment-free part of EntityPlayer.attackTargetEntityWithCurrentItem. */
    public static AttackResult createAttack(double ticksSinceLastSwing, int item,
                                            boolean sprinting, boolean falling,
                                            boolean onGround, boolean inWater,
                                            boolean flying, boolean moving) {
        float cooled = cooledAttackStrength(ticksSinceLastSwing, item, 0.5f);
        boolean strong = cooled > 0.9f;
        boolean sprintKnockback = sprinting && strong;
        boolean critical = strong && falling && !onGround && !inWater && !flying && !sprinting;
        float damage = scaledDamage(item, cooled);
        if (critical) damage *= 1.5f;
        boolean sweeping = strong && !critical && !sprintKnockback && onGround
                && !moving && isSword(item);
        return new AttackResult(cooled, damage, strong, critical, sprintKnockback, sweeping);
    }

    /** Result of EntityLivingBase's 20-tick hurt-resistance calculation. */
    public static final class DamageResult {
        public final boolean accepted;
        public final boolean newHurt;
        public final float appliedDamage;
        public final float lastDamage;
        public final double hurtResistantTime;

        DamageResult(boolean accepted, boolean newHurt, float appliedDamage,
                     float lastDamage, double hurtResistantTime) {
            this.accepted = accepted;
            this.newHurt = newHurt;
            this.appliedDamage = appliedDamage;
            this.lastDamage = lastDamage;
            this.hurtResistantTime = hurtResistantTime;
        }
    }

    public static DamageResult resolveDamage(float incomingDamage,
                                             double hurtResistantTime, float lastDamage) {
        if (incomingDamage <= 0.0f) {
            return new DamageResult(false, false, 0.0f, lastDamage, hurtResistantTime);
        }
        if (hurtResistantTime > MAX_HURT_RESISTANT_SECONDS * 0.5) {
            if (incomingDamage <= lastDamage) {
                return new DamageResult(false, false, 0.0f, lastDamage, hurtResistantTime);
            }
            return new DamageResult(true, false, incomingDamage - lastDamage,
                    incomingDamage, hurtResistantTime);
        }
        return new DamageResult(true, true, incomingDamage, incomingDamage,
                MAX_HURT_RESISTANT_SECONDS);
    }

    /** Motion values are converted from MCP blocks/tick to this game's blocks/second. */
    public static final class KnockbackResult {
        public final double motionX;
        public final double motionY;
        public final double motionZ;

        KnockbackResult(double motionX, double motionY, double motionZ) {
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;
        }
    }

    public static KnockbackResult knockBack(double motionX, double motionY, double motionZ,
                                             boolean onGround, float strength,
                                             double attackerMinusTargetX,
                                             double attackerMinusTargetZ) {
        double length = Math.sqrt(attackerMinusTargetX * attackerMinusTargetX
                + attackerMinusTargetZ * attackerMinusTargetZ);
        if (length < 1.0E-4) return new KnockbackResult(motionX, motionY, motionZ);

        motionX *= 0.5;
        motionZ *= 0.5;
        double blocksPerSecond = strength * 20.0;
        motionX -= attackerMinusTargetX / length * blocksPerSecond;
        motionZ -= attackerMinusTargetZ / length * blocksPerSecond;
        if (onGround) {
            motionY = motionY * 0.5 + blocksPerSecond;
            motionY = Math.min(motionY, 8.0); // MCP cap 0.4 blocks/tick
        }
        return new KnockbackResult(motionX, motionY, motionZ);
    }

    /** Ile dany item przywraca głodu. */
    public static int foodValue(int foodId) {
        if (foodId == ITEM_BEEF) return 8;
        if (foodId == ITEM_MUTTON) return 6;
        if (foodId == ITEM_BREAD) return 5;
        if (foodId == ITEM_PORK) return 6;
        return 0;
    }

    public static boolean isFood(int id) {
        return id == ITEM_PORK || id == ITEM_BEEF || id == ITEM_MUTTON || id == ITEM_BREAD;
    }
}
