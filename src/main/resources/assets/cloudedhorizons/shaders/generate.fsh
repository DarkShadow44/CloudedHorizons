#version 330 core
// Writes one horizontal layer (y) of the voxel volume: 1 = solid, 0 = empty.
// Same field as webdemo/index.html: 4D simplex noise above a cutout. Linked together with noise.fsh.
// uEvolve moves the noise along its time axis, which morphs the clouds in place.

uniform int uLayer;
uniform float uVS;        // voxel size in blocks
uniform vec2 uOriginXZ;   // world X/Z of the volume's minimum corner
uniform float uEvolve;

out vec4 outColor;

const float NOISE_SCALE = 256.0; // feature size in blocks
const float CUTOUT = 0.3;

float snoise4(vec4 v); // noise.fsh

void main() {
    vec2 xz = uOriginXZ + (floor(gl_FragCoord.xy) + 0.5) * uVS;
    vec3 p = vec3(xz.x, (float(uLayer) + 0.5) * uVS, xz.y);
    float n = snoise4(vec4(p / NOISE_SCALE, uEvolve));
    outColor = vec4(n > CUTOUT ? 1.0 : 0.0);
}
