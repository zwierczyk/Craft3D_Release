#version 120

// Vertex shader - passuje pozycje i tex coords przez fixed-function
void main() {
    gl_Position = gl_ProjectionMatrix * gl_ModelViewMatrix * gl_Vertex;
    gl_TexCoord[0] = gl_MultiTexCoord0;
}
