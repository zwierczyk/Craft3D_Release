#version 120
// Port 26.2 terrain.vsh (formuly z include/fog.glsl + sample_lightmap.glsl).
// Dystans mgly liczony z pozycji wierzcholka w przestrzeni widoku, nie z glebi.
varying vec2 texCoord0;
varying vec2 lightCoord;
varying float sphericalVertexDistance;
varying float cylindricalVertexDistance;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    gl_FrontColor = gl_Color;
    vec3 viewPos = (gl_ModelViewMatrix * gl_Vertex).xyz;
    sphericalVertexDistance = length(viewPos);
    cylindricalVertexDistance = max(length(viewPos.xz), abs(viewPos.y));
    texCoord0 = gl_MultiTexCoord0.xy;
    // 26.2 sample_lightmap: clamp(uv/256 + 0.5/16, 0.5/16, 15.5/16)
    lightCoord = clamp(gl_MultiTexCoord1.xy / 256.0 + vec2(0.03125), vec2(0.03125), vec2(0.96875));
}
