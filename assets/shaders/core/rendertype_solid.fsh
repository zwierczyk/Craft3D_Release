#version 120
uniform sampler2D Sampler0;      // block atlas
uniform sampler2D Sampler1;      // lightmap
uniform sampler2D Sampler3;      // shadow map (depth)
uniform vec4 ColorModulator;
uniform vec4 FogColor;
uniform float FogStart;
uniform float FogEnd;
uniform int ShadowsEnabled;
uniform float ShadowIntensity;   // 0..1 - jak mocne cienie

varying vec2 texCoord0;
varying vec2 lightCoord;
varying vec4 shadowCoord;

// PCF - Percentage-Closer Filtering: sample kilka texeli i usredniaj
float sampleShadow(vec3 projCoords) {
    float bias = 0.001;
    float shadow = 0.0;
    vec2 texelSize = vec2(1.0 / 2048.0);
    // 3x3 PCF
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            float pcfDepth = texture2D(Sampler3, projCoords.xy + vec2(x, y) * texelSize).r;
            shadow += (projCoords.z - bias > pcfDepth) ? 1.0 : 0.0;
        }
    }
    return shadow / 9.0;
}

void main() {
    vec4 tex = texture2D(Sampler0, texCoord0);
    vec4 light = texture2D(Sampler1, lightCoord);
    vec4 base = tex * gl_Color * light * ColorModulator;

    // Shadow sampling
    if (ShadowsEnabled == 1) {
        // Perspective divide + convert do [0,1] range
        vec3 projCoords = shadowCoord.xyz / shadowCoord.w;
        projCoords = projCoords * 0.5 + 0.5;
        // Sprawdz czy w light-space frustum
        if (projCoords.x >= 0.0 && projCoords.x <= 1.0
         && projCoords.y >= 0.0 && projCoords.y <= 1.0
         && projCoords.z <= 1.0) {
            float shadow = sampleShadow(projCoords);
            float shadowFactor = 1.0 - shadow * ShadowIntensity;
            base.rgb *= shadowFactor;
        }
    }

    float fogFactor = clamp((gl_FragCoord.z / gl_FragCoord.w - FogStart) / (FogEnd - FogStart), 0.0, 1.0);
    gl_FragColor = vec4(mix(base.rgb, FogColor.rgb, fogFactor), 1.0);
}
