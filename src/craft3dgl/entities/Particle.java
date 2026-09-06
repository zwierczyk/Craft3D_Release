package craft3dgl.entities;

import java.util.Random;

/**
 * A Minecraft 1.12-style particle. Motion values are blocks per game tick
 * (20 ticks per second), just like MCP 9.40's client particle classes.
 */
public final class Particle {
    public enum Type {
        BLOCK_CRACK,
        BLOCK_DUST,
        WATER_BUBBLE,
        WATER_SPLASH,
        CRIT,
        DAMAGE_INDICATOR
    }

    public final Type type;
    public double prevX, prevY, prevZ;
    public double x, y, z;
    public double motionX, motionY, motionZ;
    public float red = 1.0f, green = 1.0f, blue = 1.0f, alpha = 1.0f;
    public float scale = 1.0f;
    public int age;
    public int maxAge;
    public int textureIndex;
    public int blockId;
    public float textureJitterX;
    public float textureJitterY;
    public boolean expired;
    public boolean onGround;

    private float gravity;
    private float collisionWidth = 0.2f;
    private float collisionHeight = 0.2f;
    private Random random;

    private Particle(Type type, double x, double y, double z) {
        this.type = type;
        this.x = this.prevX = x;
        this.y = this.prevY = y;
        this.z = this.prevZ = z;
    }

    /** Equivalent to MCP Particle's randomized base constructor. */
    private static Particle base(Random random, Type type, double x, double y, double z,
                                 double xSpeed, double ySpeed, double zSpeed) {
        Particle particle = new Particle(type, x, y, z);
        particle.random = random;
        particle.motionX = xSpeed + (random.nextDouble() * 2.0 - 1.0) * 0.4;
        particle.motionY = ySpeed + (random.nextDouble() * 2.0 - 1.0) * 0.4;
        particle.motionZ = zSpeed + (random.nextDouble() * 2.0 - 1.0) * 0.4;
        double length = Math.sqrt(particle.motionX * particle.motionX
                + particle.motionY * particle.motionY + particle.motionZ * particle.motionZ);
        double speed = (random.nextDouble() + random.nextDouble() + 1.0) * 0.15;
        if (length > 1.0E-7) {
            particle.motionX = particle.motionX / length * speed * 0.4;
            particle.motionY = particle.motionY / length * speed * 0.4 + 0.1;
            particle.motionZ = particle.motionZ / length * speed * 0.4;
        }
        particle.scale = (random.nextFloat() * 0.5f + 0.5f) * 2.0f;
        particle.maxAge = Math.max(1, (int)(4.0f / (random.nextFloat() * 0.9f + 0.1f)));
        return particle;
    }

    /** MCP ParticleDigging / BLOCK_CRACK. */
    public static Particle blockCrack(Random random, double x, double y, double z,
                                      double xSpeed, double ySpeed, double zSpeed, int blockId) {
        Particle particle = base(random, Type.BLOCK_CRACK, x, y, z, xSpeed, ySpeed, zSpeed);
        particle.blockId = blockId;
        particle.gravity = 1.0f;
        particle.red = particle.green = particle.blue = 0.6f;
        particle.scale *= 0.5f;
        particle.textureJitterX = random.nextFloat() * 3.0f;
        particle.textureJitterY = random.nextFloat() * 3.0f;
        return particle;
    }

    /** MCP ParticleBlockDust. Unlike BLOCK_CRACK, this preserves the supplied velocity. */
    public static Particle blockDust(Random random, double x, double y, double z,
                                     double xSpeed, double ySpeed, double zSpeed, int blockId) {
        Particle particle = blockCrack(random, x, y, z, xSpeed, ySpeed, zSpeed, blockId);
        particle.motionX = xSpeed;
        particle.motionY = ySpeed;
        particle.motionZ = zSpeed;
        return particle.copyAs(Type.BLOCK_DUST);
    }

    /** MCP ParticleBubble / WATER_BUBBLE. */
    public static Particle bubble(Random random, double x, double y, double z,
                                  double xSpeed, double ySpeed, double zSpeed) {
        Particle particle = base(random, Type.WATER_BUBBLE, x, y, z, xSpeed, ySpeed, zSpeed);
        particle.textureIndex = 32;
        particle.collisionWidth = particle.collisionHeight = 0.02f;
        particle.scale *= random.nextFloat() * 0.6f + 0.2f;
        particle.motionX = xSpeed * 0.2 + (random.nextDouble() * 2.0 - 1.0) * 0.02;
        particle.motionY = ySpeed * 0.2 + (random.nextDouble() * 2.0 - 1.0) * 0.02;
        particle.motionZ = zSpeed * 0.2 + (random.nextDouble() * 2.0 - 1.0) * 0.02;
        particle.maxAge = Math.max(1, (int)(8.0 / (random.nextDouble() * 0.8 + 0.2)));
        return particle;
    }

    /** MCP ParticleSplash (which extends ParticleRain) / WATER_SPLASH. */
    public static Particle splash(Random random, double x, double y, double z,
                                  double xSpeed, double ySpeed, double zSpeed) {
        Particle particle = base(random, Type.WATER_SPLASH, x, y, z, 0.0, 0.0, 0.0);
        particle.motionX *= 0.3;
        particle.motionY = random.nextDouble() * 0.2 + 0.1;
        particle.motionZ *= 0.3;
        particle.textureIndex = 20 + random.nextInt(4);
        particle.collisionWidth = particle.collisionHeight = 0.01f;
        particle.gravity = 0.04f;
        particle.maxAge = Math.max(1, (int)(8.0 / (random.nextDouble() * 0.8 + 0.2)));
        if (ySpeed == 0.0 && (xSpeed != 0.0 || zSpeed != 0.0)) {
            particle.motionX = xSpeed;
            particle.motionY = 0.1;
            particle.motionZ = zSpeed;
        }
        return particle;
    }

    /** MCP ParticleCrit / CRIT. */
    public static Particle crit(Random random, double x, double y, double z,
                                double xSpeed, double ySpeed, double zSpeed) {
        Particle particle = base(random, Type.CRIT, x, y, z, 0.0, 0.0, 0.0);
        particle.motionX = particle.motionX * 0.1 + xSpeed * 0.4;
        particle.motionY = particle.motionY * 0.1 + ySpeed * 0.4;
        particle.motionZ = particle.motionZ * 0.1 + zSpeed * 0.4;
        float shade = random.nextFloat() * 0.3f + 0.6f;
        particle.red = particle.green = shade;
        particle.blue = particle.red;
        particle.scale *= 0.75f;
        particle.maxAge = Math.max(1, (int)(6.0f / (random.nextFloat() * 0.8f + 0.6f)));
        particle.textureIndex = 65;
        // ParticleCrit advances once in its constructor in Minecraft 1.12.
        particle.tick(null);
        return particle;
    }

    /** MCP DAMAGE_INDICATOR factory: ParticleCrit with sprite 67, +1 Y speed and 20 ticks. */
    public static Particle damageIndicator(Random random, double x, double y, double z,
                                           double xSpeed, double ySpeed, double zSpeed) {
        Particle crit = crit(random, x, y, z, xSpeed, ySpeed + 1.0, zSpeed);
        Particle particle = crit.copyAs(Type.DAMAGE_INDICATOR);
        particle.textureIndex = 67;
        particle.maxAge = 20;
        return particle;
    }

    private Particle copyAs(Type newType) {
        Particle copy = new Particle(newType, x, y, z);
        copy.prevX = prevX;
        copy.prevY = prevY;
        copy.prevZ = prevZ;
        copy.motionX = motionX;
        copy.motionY = motionY;
        copy.motionZ = motionZ;
        copy.red = red;
        copy.green = green;
        copy.blue = blue;
        copy.alpha = alpha;
        copy.scale = scale;
        copy.age = age;
        copy.maxAge = maxAge;
        copy.textureIndex = textureIndex;
        copy.blockId = blockId;
        copy.textureJitterX = textureJitterX;
        copy.textureJitterY = textureJitterY;
        copy.expired = expired;
        copy.onGround = onGround;
        copy.gravity = gravity;
        copy.collisionWidth = collisionWidth;
        copy.collisionHeight = collisionHeight;
        copy.random = random;
        return copy;
    }

    public Particle multiplyVelocity(float multiplier) {
        motionX *= multiplier;
        motionY = (motionY - 0.1) * multiplier + 0.1;
        motionZ *= multiplier;
        return this;
    }

    public Particle multiplyScale(float multiplier) {
        collisionWidth = 0.2f * multiplier;
        collisionHeight = 0.2f * multiplier;
        scale *= multiplier;
        return this;
    }

    public boolean usesBlockAtlas() {
        return type == Type.BLOCK_CRACK || type == Type.BLOCK_DUST;
    }

    public float renderScale(float partialTicks) {
        if (type == Type.CRIT || type == Type.DAMAGE_INDICATOR) {
            float growth = ((float)age + partialTicks) / (float)Math.max(1, maxAge) * 32.0f;
            return scale * Math.min(growth, 1.0f);
        }
        return scale;
    }

    public void tick(ParticleSystem.WorldAccess world) {
        prevX = x;
        prevY = y;
        prevZ = z;

        switch (type) {
            case WATER_BUBBLE:
                motionY += 0.002;
                move(world, motionX, motionY, motionZ);
                motionX *= 0.85;
                motionY *= 0.85;
                motionZ *= 0.85;
                if (world != null && !world.isWater(x, y, z)) expired = true;
                if (maxAge-- <= 0) expired = true;
                break;

            case WATER_SPLASH:
                motionY -= gravity;
                move(world, motionX, motionY, motionZ);
                motionX *= 0.98;
                motionY *= 0.98;
                motionZ *= 0.98;
                if (maxAge-- <= 0) expired = true;
                if (onGround) {
                    if (random != null && random.nextDouble() < 0.5) expired = true;
                    motionX *= 0.7;
                    motionZ *= 0.7;
                }
                if (world != null && world.isWater(x, y, z)) expired = true;
                break;

            case CRIT:
            case DAMAGE_INDICATOR:
                if (age++ >= maxAge) expired = true;
                move(world, motionX, motionY, motionZ);
                green *= 0.96f;
                blue *= 0.9f;
                motionX *= 0.7;
                motionY = motionY * 0.7 - 0.02;
                motionZ *= 0.7;
                break;

            case BLOCK_CRACK:
            case BLOCK_DUST:
                if (age++ >= maxAge) expired = true;
                motionY -= 0.04 * gravity;
                move(world, motionX, motionY, motionZ);
                motionX *= 0.98;
                motionY *= 0.98;
                motionZ *= 0.98;
                if (onGround) {
                    motionX *= 0.7;
                    motionZ *= 0.7;
                }
                break;
        }
    }

    private void move(ParticleSystem.WorldAccess world, double dx, double dy, double dz) {
        onGround = false;
        if (world == null) {
            x += dx;
            y += dy;
            z += dz;
            return;
        }

        double radius = collisionWidth * 0.5;
        if (!verticalCollision(world, y + dy, dy, radius)) {
            y += dy;
        } else {
            if (dy < 0.0) onGround = true;
            motionY = 0.0;
        }
        if (!xCollision(world, x + dx, dx, radius)) {
            x += dx;
        } else {
            motionX = 0.0;
        }
        if (!zCollision(world, z + dz, dz, radius)) {
            z += dz;
        } else {
            motionZ = 0.0;
        }
    }

    private boolean verticalCollision(ParticleSystem.WorldAccess world, double nextY,
                                      double dy, double radius) {
        if (dy == 0.0) return false;
        double sampleY = dy < 0.0 ? nextY + 1.0E-7 : nextY + collisionHeight - 1.0E-7;
        return solidAtHorizontalSlice(world, x, sampleY, z, radius);
    }

    private boolean xCollision(ParticleSystem.WorldAccess world, double nextX,
                               double dx, double radius) {
        if (dx == 0.0) return false;
        double sampleX = nextX + (dx > 0.0 ? radius - 1.0E-7 : -radius + 1.0E-7);
        double y0 = y + 1.0E-7;
        double y1 = y + collisionHeight - 1.0E-7;
        return world.isSolid(sampleX, y0, z - radius) || world.isSolid(sampleX, y0, z + radius)
                || world.isSolid(sampleX, y1, z - radius) || world.isSolid(sampleX, y1, z + radius)
                || world.isSolid(sampleX, y + collisionHeight * 0.5, z);
    }

    private boolean zCollision(ParticleSystem.WorldAccess world, double nextZ,
                               double dz, double radius) {
        if (dz == 0.0) return false;
        double sampleZ = nextZ + (dz > 0.0 ? radius - 1.0E-7 : -radius + 1.0E-7);
        double y0 = y + 1.0E-7;
        double y1 = y + collisionHeight - 1.0E-7;
        return world.isSolid(x - radius, y0, sampleZ) || world.isSolid(x + radius, y0, sampleZ)
                || world.isSolid(x - radius, y1, sampleZ) || world.isSolid(x + radius, y1, sampleZ)
                || world.isSolid(x, y + collisionHeight * 0.5, sampleZ);
    }

    private static boolean solidAtHorizontalSlice(ParticleSystem.WorldAccess world,
                                                  double x, double y, double z, double radius) {
        return world.isSolid(x - radius, y, z - radius) || world.isSolid(x + radius, y, z - radius)
                || world.isSolid(x - radius, y, z + radius) || world.isSolid(x + radius, y, z + radius)
                || world.isSolid(x, y, z);
    }

}
