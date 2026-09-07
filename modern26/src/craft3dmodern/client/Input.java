package craft3dmodern.client;

/** Zbior wejscia jednej klatki (mysz, klawisze). Uzupelnia Main. */
public final class Input {
    public float mouseX, mouseY;          // pozycja kursora (piksele okna)
    public double mouseDx, mouseDy;       // delta kursora (gdy przechwycony)
    public boolean leftDown, escDown;
    public boolean forward, back, strafeLeft, strafeRight;
    public boolean up, down, sprint, regen;
}
