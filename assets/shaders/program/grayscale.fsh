#version 120

uniform sampler2D DiffuseSampler;

void main() {
    vec4 c = texture2D(DiffuseSampler, gl_TexCoord[0].xy);
    float luma = dot(c.rgb, vec3(0.299, 0.587, 0.114));
    gl_FragColor = vec4(luma, luma, luma, c.a);
}
