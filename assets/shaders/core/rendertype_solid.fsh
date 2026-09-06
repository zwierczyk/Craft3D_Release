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
    vec4 base = texture2D(Sampler0, texCoord0) * gl_Color
              * texture2D(Sampler1, lightCoord) * ColorModulator;
    float fogDistance = gl_FragCoord.z / gl_FragCoord.w;
    float fogFactor = clamp((fogDistance - FogStart) / (FogEnd - FogStart), 0.0, 1.0);
    gl_FragColor = vec4(mix(base.rgb, FogColor.rgb, fogFactor), 1.0);
}
