#version 120

uniform sampler2D DiffuseSampler;

void main() {
    vec4 c = texture2D(DiffuseSampler, gl_TexCoord[0].xy);
    // Prawdziwy negatyw - odwrocenie kolorow
    gl_FragColor = vec4(1.0 - c.rgb, c.a);
}
