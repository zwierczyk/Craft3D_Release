#version 120
// rendertype_solid z shadow support
uniform mat4 LightSpaceMatrix;   // LightViewProjection z pozycji slonca
uniform int ShadowsEnabled;      // 0 = brak cieni, 1 = z shadow map

varying vec2 texCoord0;
varying vec2 lightCoord;
varying vec4 shadowCoord;        // pozycja pixela w light-space (dla shadow sampling)

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    gl_FrontColor = gl_Color;
    texCoord0 = gl_MultiTexCoord0.xy;
    lightCoord = gl_MultiTexCoord1.xy / 256.0;
    // Pozycja w world-space: dla legacy pipeline uzywamy gl_Vertex bo modelview
    // jest tylko view matrix (translation gracza) - vertex jest juz w world coords
    // Trzeba przemnozyc przez INVERSE ViewMatrix zeby uzyskac world, ale prosciej:
    // shadowCoord = LightSpaceMatrix * gl_Vertex (bo nasze vertexy sa juz w world)
    shadowCoord = LightSpaceMatrix * gl_Vertex;
}
