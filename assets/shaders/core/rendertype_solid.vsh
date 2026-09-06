#version 120
varying vec2 texCoord0;
varying vec2 lightCoord;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    gl_FrontColor = gl_Color;
    texCoord0 = gl_MultiTexCoord0.xy;
    lightCoord = gl_MultiTexCoord1.xy / 256.0;
}
