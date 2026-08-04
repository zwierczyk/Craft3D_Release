#version 120
// core/position - tylko pozycja, uniform color modulate
void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}
