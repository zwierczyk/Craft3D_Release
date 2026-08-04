#version 120
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
void main() {
    vec4 c = texture2D(Sampler0, gl_TexCoord[0].xy);
    if (c.a == 0.0) discard;
    gl_FragColor = c * ColorModulator;
}
