#version 120
// Port 26.2 terrain.fsh + fog.glsl apply_fog (woda / przezroczyste).
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec4 ColorModulator;
uniform vec4 FogColor;
uniform float FogStart;
uniform float FogEnd;

varying vec2 texCoord0;
varying vec2 lightCoord;
varying float sphericalVertexDistance;
varying float cylindricalVertexDistance;

float linear_fog_value(float dist, float fogStart, float fogEnd) {
    if (dist <= fogStart) return 0.0;
    if (dist >= fogEnd) return 1.0;
    return (dist - fogStart) / (fogEnd - fogStart);
}

void main() {
    vec4 tex = texture2D(Sampler0, texCoord0);
    vec4 light = texture2D(Sampler1, lightCoord);
    vec4 base = tex * gl_Color * light * ColorModulator;
    if (base.a < 0.01) discard;
    // Render-distance pair (26.2 fog.glsl) works on the cylindrical distance.
    float fogValue = linear_fog_value(cylindricalVertexDistance, FogStart, FogEnd);
    gl_FragColor = vec4(mix(base.rgb, FogColor.rgb, fogValue * FogColor.a), base.a);
}
