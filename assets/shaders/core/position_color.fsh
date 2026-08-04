#version 120
uniform vec4 ColorModulator;
void main() {
    gl_FragColor = gl_Color * ColorModulator;
}
