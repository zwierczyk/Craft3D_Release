#version 120

uniform sampler2D DiffuseSampler;

void main() {
    vec2 texel = vec2(1.0 / 1920.0, 1.0 / 1080.0);
    vec4 sum = vec4(0.0);
    float r = 2.0;
    for (float x = -r; x <= r; x++) {
        for (float y = -r; y <= r; y++) {
            sum += texture2D(DiffuseSampler, gl_TexCoord[0].xy + vec2(x, y) * texel);
        }
    }
    float samples = (2.0 * r + 1.0) * (2.0 * r + 1.0);
    gl_FragColor = sum / samples;
}
