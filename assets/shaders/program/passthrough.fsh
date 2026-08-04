#version 120

// Passthrough - kopiuje FBO texture 1:1 na ekran
uniform sampler2D DiffuseSampler;

void main() {
    // gl_TexCoord[0] przekazane z vertex shadera (gl_MultiTexCoord0)
    gl_FragColor = texture2D(DiffuseSampler, gl_TexCoord[0].xy);
}
