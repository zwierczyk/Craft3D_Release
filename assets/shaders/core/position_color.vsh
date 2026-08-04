#version 120
// core/position_color - pozycja + per-vertex color
void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    gl_FrontColor = gl_Color;
}
