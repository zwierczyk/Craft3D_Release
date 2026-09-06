#version 120
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec4 ColorModulator;
uniform vec4 FogColor;
uniform float FogStart;
uniform float FogEnd;
varying vec2 texCoord0;
varying vec2 lightCoord;
void main() {
    vec4 tex = texture2D(Sampler0, texCoord0);
    if (tex.a < 0.1) discard;                     // Minecraft 1.12 alpha threshold
    vec4 light = texture2D(Sampler1, lightCoord);
    vec4 base = tex * gl_Color * light * ColorModulator;
    float fogFactor = clamp((gl_FragCoord.z / gl_FragCoord.w - FogStart) / (FogEnd - FogStart), 0.0, 1.0);
    gl_FragColor = mix(base, vec4(FogColor.rgb, base.a), fogFactor);
}
