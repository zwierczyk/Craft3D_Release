package craft3dgl.world;

import craft3dgl.ui.TextureAtlas;

import static craft3dgl.world.WorldConstants.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Renderer modelu debowych drzwi z Minecraft 1.12.
 *
 * Bazowy model jest panelem 3x16x16 px. Ma dwie teksturowane duze strony,
 * dwie waskie krawedzie i zewnetrzny cap na dole/gorze. Pomiedzy polowkami
 * nie ma dodatkowej scianki. Przez przezroczyste okna naprawde widac swiat.
 */
public final class DoorRenderer {
    private DoorRenderer() {}

    public static void drawAll(byte[][][] world, DoorSystem doors,
                               double playerX, double playerZ,
                               int textureAtlas, int range,
                               LightEngine lightEngine, float dayMult) {
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, textureAtlas);
        float dayTint = 0.20f + dayMult * 0.80f;
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.5f);

        int minX = Math.max(0, (int)playerX - range);
        int maxX = Math.min(WORLD_X - 1, (int)playerX + range);
        int minZ = Math.max(0, (int)playerZ - range);
        int maxZ = Math.min(WORLD_Z - 1, (int)playerZ + range);

        glBegin(GL_QUADS);
        for (int x = minX; x <= maxX; x++) {
            for (int y = 0; y < WORLD_Y; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int id = world[x][y][z] & 0xff;
                    if (id == DOOR_BOTTOM || id == DOOR_TOP) {
                        drawDoorHalf(doors, x, y, z, id, lightEngine, dayMult, dayTint);
                    }
                }
            }
        }
        glEnd();
        glDisable(GL_ALPHA_TEST);
        glColor4f(1, 1, 1, 1);
    }

    public static void drawAll(byte[][][] world, DoorSystem doors,
                               double playerX, double playerZ,
                               int textureAtlas, int range) {
        drawAll(world, doors, playerX, playerZ, textureAtlas, range, null, 1.0f);
    }

    private static void drawDoorHalf(DoorSystem doors, int x, int y, int z, int id,
                                     LightEngine lightEngine, float dayMult, float dayTint) {
        int meta = doors.getMeta(x, y, z);
        int facing = DoorSystem.facing(meta);
        boolean open = DoorSystem.isOpen(meta);
        boolean rightHinge = DoorSystem.isRightHinge(meta);

        // Odpowiednik wariantow wooden_door_bottom(_rh) i obrotow blockstate.
        boolean rightHandModel = rightHinge ^ open;
        int rotation;
        if (!open) rotation = (facing + 1) & 3;
        else rotation = rightHinge ? facing : ((facing + 2) & 3);

        float environment = lightEngine == null
                ? 1.0f : lightEngine.sampleShade(x, y, z, dayMult);
        float faceLight = environment * dayTint;
        float edgeLight = faceLight * 0.82f;

        int tile = id == DOOR_BOTTOM ? 12 : 13;
        double u0 = TextureAtlas.atlasU0(tile);
        double u1 = TextureAtlas.atlasU1(tile);
        double v0 = TextureAtlas.atlasV0();
        double v1 = TextureAtlas.atlasV1();
        double edgeU = u0 + (u1 - u0) * (3.0 / 16.0);
        double largeU0 = rightHandModel ? u1 : u0;
        double largeU1 = rightHandModel ? u0 : u1;
        double thickness = DoorSystem.THICKNESS;

        // WEST duza strona bazowego modelu.
        glColor3f(faceLight, faceLight, faceLight);
        texVertex(largeU0, v1, x, y, z, 0, 0, 0, rotation);
        texVertex(largeU1, v1, x, y, z, 0, 0, 1, rotation);
        texVertex(largeU1, v0, x, y, z, 0, 1, 1, rotation);
        texVertex(largeU0, v0, x, y, z, 0, 1, 0, rotation);

        // EAST duza strona; odwrotna kolejnosc daje lustrzany tyl tekstury.
        texVertex(largeU0, v1, x, y, z, thickness, 0, 1, rotation);
        texVertex(largeU1, v1, x, y, z, thickness, 0, 0, rotation);
        texVertex(largeU1, v0, x, y, z, thickness, 1, 0, rotation);
        texVertex(largeU0, v0, x, y, z, thickness, 1, 1, rotation);

        // NORTH/SOUTH - waskie krawedzie z 3-pikselowych paskow tekstury.
        glColor3f(edgeLight, edgeLight, edgeLight);
        texVertex(edgeU, v1, x, y, z, thickness, 0, 0, rotation);
        texVertex(u0, v1, x, y, z, 0, 0, 0, rotation);
        texVertex(u0, v0, x, y, z, 0, 1, 0, rotation);
        texVertex(edgeU, v0, x, y, z, thickness, 1, 0, rotation);

        texVertex(u0, v1, x, y, z, 0, 0, 1, rotation);
        texVertex(edgeU, v1, x, y, z, thickness, 0, 1, rotation);
        texVertex(edgeU, v0, x, y, z, thickness, 1, 1, rotation);
        texVertex(u0, v0, x, y, z, 0, 1, 1, rotation);

        // Vanilla nie rysuje scianki pomiedzy dwiema polowkami drzwi.
        // Dolny cap wystepuje tylko w lower, a gorny tylko w upper.
        if (id == DOOR_BOTTOM) {
            double capU0 = u0 + (u1 - u0) * (13.0 / 16.0);
            glColor3f(edgeLight, edgeLight, edgeLight);
            texVertex(capU0, v0, x, y, z, 0, 0, 0, rotation);
            texVertex(u1, v0, x, y, z, thickness, 0, 0, rotation);
            texVertex(u1, v1, x, y, z, thickness, 0, 1, rotation);
            texVertex(capU0, v1, x, y, z, 0, 0, 1, rotation);
        } else {
            // Model top uzywa tekstury dolnej polowki dla waskiego top capa.
            double capTileU0 = TextureAtlas.atlasU0(12);
            double capTileU1 = TextureAtlas.atlasU1(12);
            double capU0 = capTileU0 + (capTileU1 - capTileU0) * (13.0 / 16.0);
            glColor3f(edgeLight, edgeLight, edgeLight);
            texVertex(capU0, v1, x, y, z, 0, 1, 1, rotation);
            texVertex(capTileU1, v1, x, y, z, thickness, 1, 1, rotation);
            texVertex(capTileU1, v0, x, y, z, thickness, 1, 0, rotation);
            texVertex(capU0, v0, x, y, z, 0, 1, 0, rotation);
        }
    }

    /** Obraca wierzcholek bazowego modelu wokol srodka bloku co 90 stopni. */
    private static void texVertex(double u, double v,
                                  int blockX, int blockY, int blockZ,
                                  double localX, double localY, double localZ,
                                  int rotation) {
        double x;
        double z;
        switch (rotation & 3) {
            case 1:
                x = 1.0 - localZ;
                z = localX;
                break;
            case 2:
                x = 1.0 - localX;
                z = 1.0 - localZ;
                break;
            case 3:
                x = localZ;
                z = 1.0 - localX;
                break;
            default:
                x = localX;
                z = localZ;
                break;
        }
        glTexCoord2d(u, v);
        glVertex3d(blockX + x, blockY + localY, blockZ + z);
    }
}
