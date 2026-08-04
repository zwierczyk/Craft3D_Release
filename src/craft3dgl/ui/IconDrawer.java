package craft3dgl.ui;

/**
 * Callback do rysowania ikony itemu w slocie. Implementowany przez MinecraftGL
 * bo wymaga dostepu do textury atlas + lokalnych metod ikon (tools).
 */
@FunctionalInterface
public interface IconDrawer {
    void drawStackIcon(int id, int count, int x, int y, int size);
}
