#version 120
// Shadow depth pass - tylko zapisujemy glebie (depth-only render)
// Uzywamy standard MVP z light space
void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}
