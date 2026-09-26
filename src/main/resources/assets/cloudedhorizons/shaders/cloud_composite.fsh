#version 330 core
// One additive texture: weighted light, total weight, and summed optical depth.
uniform sampler2D uAccumulation;
uniform vec3 uColour;
uniform ivec2 uViewportOrigin;
out vec4 outColor;

void main() {
    ivec2 pixel = ivec2(gl_FragCoord.xy) - uViewportOrigin;
    vec3 accum = texelFetch(uAccumulation, pixel, 0).rgb;
    if (accum.g <= 0.0) discard;
    float light = accum.r / accum.g;
    float alpha = 1.0 - exp(-accum.b);
    outColor = vec4(uColour * light, alpha);
}
