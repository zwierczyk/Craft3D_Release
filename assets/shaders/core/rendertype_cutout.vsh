#version 120
// rendertype_cutout z WIND SWAY dla lisci/trawy/wheat
uniform float GameTime;   // sekundy od startu gry
varying vec2 texCoord0;
varying vec2 lightCoord;

void main() {
    vec4 pos = gl_Vertex;

    // WIND SWAY: tylko dla vertexow na GORNEJ czesci bloku
    // Trik MC: gdy UV.v jest MALE (0-0.5) to vertex jest na gornej krawedzi tekstury
    // = fizycznie u gory bloku (np. korony trawy, gora lisci)
    // Nizej (v > 0.5) = dol - nie kolyszemy
    // Ale nasze bloki uzywaja pelnej tekstury 0..1 wiec: v<0.5 = gora
    float uvTop = 1.0 - clamp(gl_MultiTexCoord0.y * 2.0, 0.0, 1.0);  // 1 na gorze, 0 na dole
    // Wektor wiatru na podstawie WORLD position + czas
    // (dzieki world position rozne bloki kolyszą sie roznie - nie synchronicznie)
    float windPhase = gl_Vertex.x * 0.3 + gl_Vertex.z * 0.5 + GameTime * 2.5;
    float windX = sin(windPhase) * 0.08 * uvTop;
    float windZ = cos(windPhase * 0.7) * 0.06 * uvTop;
    pos.x += windX;
    pos.z += windZ;
    // Lekkie oscylacje w Y (kolysanie w gore/dol)
    pos.y += sin(windPhase * 1.3) * 0.02 * uvTop;

    gl_Position = gl_ModelViewProjectionMatrix * pos;
    gl_FrontColor = gl_Color;
    texCoord0 = gl_MultiTexCoord0.xy;
    lightCoord = gl_MultiTexCoord1.xy / 256.0;
}
