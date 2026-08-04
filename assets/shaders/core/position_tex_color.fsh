#version 120
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
void main() {
    vec4 tex = texture2D(Sampler0, gl_TexCoord[0].xy);
    gl_FragColor = tex * gl_Color * ColorModulator;
}
