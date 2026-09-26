#version 330 core
// Writes one horizontal layer (y) of the voxel volume: 1 = solid, 0 = empty.
// Same field as webdemo/index.html: 4D simplex noise above a cutout. Linked together with noise.fsh.
// uEvolve moves the noise along its time axis, which morphs the clouds in place.

uniform int uLayer;
uniform float uVS;        // voxel size in blocks
uniform vec2 uOriginXZ;   // world X/Z of the volume's minimum corner
uniform float uEvolve;
uniform vec3 uNoiseScale; // noise feature size per axis (x, y, z), in voxels

out vec4 outColor;

const float CUTOUT = 0.3;

float snoise4(vec4 v); // noise.fsh

void main() {
    // Voxel coordinates (x, y, z); the volume origin is a whole number of voxels.
    vec2 xz = uOriginXZ / uVS + floor(gl_FragCoord.xy) + 0.5;
    vec3 v = vec3(xz.x, float(uLayer) + 0.5, xz.y);
    float n = snoise4(vec4(v / max(uNoiseScale, vec3(1e-3)), uEvolve));
    outColor = vec4(n > CUTOUT ? 1.0 : 0.0);
}
