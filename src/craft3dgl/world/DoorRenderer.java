package craft3dgl.world;

import craft3dgl.ui.TextureAtlas;

import static craft3dgl.world.WorldConstants.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Renderer drzwi MC-style:
 *  - 3px grubosci (jak MC)
 *  - PRZOD/TYL z pelna teksturą (okienka widoczne)
 *  - WEWNETRZNY SOLIDNY quad na srodku - wypelnia okienka drewnem
 *    (uzywamy waskiego paska UV z solidnej strony tekstury)
 *  - Boki (top/bottom/left/right caps) POMINIETE zeby przez okienka nie
 *    bylo widac wnetrza pustego pudelka.
 */
public final class DoorRenderer {
    private DoorRenderer() {}

    // Waski pasek UV z solidnej strony tekstury (lewa krawedz 3px = zawsze opaque drewno)
    private static final double SOLID_UV_STRIP = 3.0 / 16.0;

    public static void drawAll(byte[][][] world, DoorSystem doors,
                                double playerX, double playerZ,
                                int textureAtlas, int range,
                                LightEngine lightEngine, float dayMult) {
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, textureAtlas);
        float dayTint = 0.20f + dayMult * 0.80f;
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.5f);
        int minBx = Math.max(0, (int) playerX - range);
        int maxBx = Math.min(WORLD_X - 1, (int) playerX + range);
        int minBz = Math.max(0, (int) playerZ - range);
        int maxBz = Math.min(WORLD_Z - 1, (int) playerZ + range);
        glBegin(GL_QUADS);
        for (int bx = minBx; bx <= maxBx; bx++)
            for (int by = 0; by < WORLD_Y; by++)
                for (int bz = minBz; bz <= maxBz; bz++) {
                    int id = world[bx][by][bz] & 0xff;
                    if (id != DOOR_BOTTOM && id != DOOR_TOP) continue;
                    drawDoorBlock(doors, bx, by, bz, id, lightEngine, dayMult, dayTint);
                }
        glEnd();
        glDisable(GL_ALPHA_TEST);
    }

    public static void drawAll(byte[][][] world, DoorSystem doors,
                                double playerX, double playerZ,
                                int textureAtlas, int range) {
        drawAll(world, doors, playerX, playerZ, textureAtlas, range, null, 1.0f);
    }

    private static void drawDoorBlock(DoorSystem doors,
                                       int bx, int by, int bz, int id,
                                       LightEngine lightEngine, float dayMult, float dayTint) {
        int meta = doors.getMeta(bx, by, bz);
        double[] aabb = DoorSystem.doorAabb(meta);
        double x0 = bx + aabb[0], z0 = bz + aabb[1];
        double x1 = bx + aabb[2], z1 = bz + aabb[3];
        double y0 = by, y1 = by + 1;
        double dx = aabb[2] - aabb[0];
        double dz = aabb[3] - aabb[1];
        boolean wideOnX = dx > dz;

        int tile = id == DOOR_BOTTOM ? 12 : 13;
        double u0 = TextureAtlas.atlasU0(tile);
        double u1 = TextureAtlas.atlasU1(tile);
        double v0 = TextureAtlas.atlasV0();
        double v1 = TextureAtlas.atlasV1();
        // Waski pasek UV (solidny, bez okienek - z lewej krawedzi tekstury)
        double uSol0 = u0;
        double uSol1 = u0 + (u1 - u0) * SOLID_UV_STRIP;

        float envLight = 1.0f;
        if (lightEngine != null) {
            envLight = lightEngine.sampleShade(bx, by, bz, dayMult);
        }
        float faceShade = 1.00f * envLight * dayTint;
        float coreShade = 0.90f * envLight * dayTint;
        glColor3f(faceShade, faceShade, faceShade);

        if (wideOnX) {
            double zM = (z0 + z1) * 0.5;
            // PRZOD +Z (na z1, pelna tekstura z okienkami)
            glTexCoord2d(u0,v1); glVertex3d(x0,y0,z1);
            glTexCoord2d(u1,v1); glVertex3d(x1,y0,z1);
            glTexCoord2d(u1,v0); glVertex3d(x1,y1,z1);
            glTexCoord2d(u0,v0); glVertex3d(x0,y1,z1);
            // TYL -Z (na z0, mirror)
            glTexCoord2d(u1,v1); glVertex3d(x0,y0,z0);
            glTexCoord2d(u0,v1); glVertex3d(x1,y0,z0);
            glTexCoord2d(u0,v0); glVertex3d(x1,y1,z0);
            glTexCoord2d(u1,v0); glVertex3d(x0,y1,z0);
            // WEWNETRZNY SOLIDNY quad na srodku - wypelnia okienka drewnem
            glColor3f(coreShade, coreShade, coreShade);
            // Rysujemy dwa razy (2 orientacje) zeby widac z obu stron
            glTexCoord2d(uSol0,v1); glVertex3d(x0,y0,zM);
            glTexCoord2d(uSol1,v1); glVertex3d(x1,y0,zM);
            glTexCoord2d(uSol1,v0); glVertex3d(x1,y1,zM);
            glTexCoord2d(uSol0,v0); glVertex3d(x0,y1,zM);
            glTexCoord2d(uSol1,v1); glVertex3d(x0,y0,zM);
            glTexCoord2d(uSol0,v1); glVertex3d(x1,y0,zM);
            glTexCoord2d(uSol0,v0); glVertex3d(x1,y1,zM);
            glTexCoord2d(uSol1,v0); glVertex3d(x0,y1,zM);
        } else {
            double xM = (x0 + x1) * 0.5;
            // PRZOD +X (na x1)
            glTexCoord2d(u1,v1); glVertex3d(x1,y0,z0);
            glTexCoord2d(u0,v1); glVertex3d(x1,y0,z1);
            glTexCoord2d(u0,v0); glVertex3d(x1,y1,z1);
            glTexCoord2d(u1,v0); glVertex3d(x1,y1,z0);
            // TYL -X (na x0, mirror)
            glTexCoord2d(u0,v1); glVertex3d(x0,y0,z0);
            glTexCoord2d(u1,v1); glVertex3d(x0,y0,z1);
            glTexCoord2d(u1,v0); glVertex3d(x0,y1,z1);
            glTexCoord2d(u0,v0); glVertex3d(x0,y1,z0);
            // WEWNETRZNY SOLIDNY quad na srodku
            glColor3f(coreShade, coreShade, coreShade);
            glTexCoord2d(uSol1,v1); glVertex3d(xM,y0,z0);
            glTexCoord2d(uSol0,v1); glVertex3d(xM,y0,z1);
            glTexCoord2d(uSol0,v0); glVertex3d(xM,y1,z1);
            glTexCoord2d(uSol1,v0); glVertex3d(xM,y1,z0);
            glTexCoord2d(uSol0,v1); glVertex3d(xM,y0,z0);
            glTexCoord2d(uSol1,v1); glVertex3d(xM,y0,z1);
            glTexCoord2d(uSol1,v0); glVertex3d(xM,y1,z1);
            glTexCoord2d(uSol0,v0); glVertex3d(xM,y1,z0);
        }
    }
}
