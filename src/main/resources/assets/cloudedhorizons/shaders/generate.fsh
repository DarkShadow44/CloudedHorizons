#version 330 core
// Writes one horizontal layer (y) of the voxel volume: 1 = solid, 0 = empty.
// Same field as webdemo/index.html (column field + height profile + detail), without wind.
// uEvolve moves the noise along its time axis, which morphs the clouds in place.

uniform int uLayer;
uniform int uHeight;      // voxel layers in the volume
uniform float uVS;        // voxel size in blocks
uniform vec2 uOriginXZ;   // world X/Z of the volume's minimum corner
uniform uint uSeed;
uniform float uEvolve;

out vec4 outColor;

const float COVERAGE = 0.42;
const float MAP_SCALE = 520.0;
const int OCTAVES = 5;
const float GAIN = 0.5;
const float CONTRAST = 1.3;
const float WARP_SCALE = 900.0;
const float WARP_STRENGTH = 250.0;
const float REGION_SCALE = 4500.0;
const float REGION_STRENGTH = 0.35;
const float TOWER = 3.0;
const float ROUNDNESS = 1.2;
const float BASE_FLAT = 0.6;
const float DETAIL_SCALE = 90.0;
const float DETAIL_STRENGTH = 0.22;
const int DETAIL_OCTAVES = 2;

uvec3 pcg3d(uvec3 v) {
    v = v * 1664525u + 1013904223u;
    v.x += v.y * v.z; v.y += v.z * v.x; v.z += v.x * v.y;
    v ^= v >> 16u;
    v.x += v.y * v.z; v.y += v.z * v.x; v.z += v.x * v.y;
    return v;
}

vec3 grad3(ivec3 p) {
    uvec3 h = pcg3d(uvec3(p) + uvec3(uSeed * 7919u, uSeed * 104729u, uSeed * 15485863u));
    return vec3(h & 0xffffu) * (2.0 / 65535.0) - 1.0;
}

float gnoise(vec3 x) {
    vec3 fl = floor(x);
    ivec3 i = ivec3(fl);
    vec3 f = x - fl;
    vec3 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    float n000 = dot(grad3(i + ivec3(0, 0, 0)), f - vec3(0, 0, 0));
    float n100 = dot(grad3(i + ivec3(1, 0, 0)), f - vec3(1, 0, 0));
    float n010 = dot(grad3(i + ivec3(0, 1, 0)), f - vec3(0, 1, 0));
    float n110 = dot(grad3(i + ivec3(1, 1, 0)), f - vec3(1, 1, 0));
    float n001 = dot(grad3(i + ivec3(0, 0, 1)), f - vec3(0, 0, 1));
    float n101 = dot(grad3(i + ivec3(1, 0, 1)), f - vec3(1, 0, 1));
    float n011 = dot(grad3(i + ivec3(0, 1, 1)), f - vec3(0, 1, 1));
    float n111 = dot(grad3(i + ivec3(1, 1, 1)), f - vec3(1, 1, 1));
    return mix(mix(mix(n000, n100, u.x), mix(n010, n110, u.x), u.y),
               mix(mix(n001, n101, u.x), mix(n011, n111, u.x), u.y), u.z) * 1.4;
}

float fbm(vec3 p, int octaves, float gain) {
    float sum = 0.0, amp = 1.0, norm = 0.0;
    for (int i = 0; i < 8; i++) {
        if (i >= octaves) break;
        sum += amp * gnoise(p);
        norm += amp;
        amp *= gain;
        p = p * 2.03 + vec3(17.13, -9.71, 5.37);
    }
    return norm > 0.0 ? sum / norm : 0.0;
}

uvec4 pcg4d(uvec4 v) {
    v = v * 1664525u + 1013904223u;
    v.x += v.y * v.w; v.y += v.z * v.x; v.z += v.x * v.y; v.w += v.y * v.z;
    v ^= v >> 16u;
    v.x += v.y * v.w; v.y += v.z * v.x; v.z += v.x * v.y; v.w += v.y * v.z;
    return v;
}

float corner4(ivec4 i, vec4 f, ivec4 o) {
    uvec4 h = pcg4d(uvec4(i + o) + uvec4(uSeed * 7919u, uSeed * 104729u, uSeed * 15485863u, uSeed * 32452843u));
    vec4 g = vec4(h & 0xffffu) * (2.0 / 65535.0) - 1.0;
    return dot(g, f - vec4(o));
}

// 4D gradient noise in roughly [-1, 1]. The fourth axis is time: moving along it changes a 3D field in place
// without translating it in space.
float gnoise4(vec4 x) {
    vec4 fl = floor(x);
    ivec4 i = ivec4(fl);
    vec4 f = x - fl;
    vec4 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    float r[2];
    for (int w = 0; w < 2; w++) {
        float n000 = corner4(i, f, ivec4(0, 0, 0, w));
        float n100 = corner4(i, f, ivec4(1, 0, 0, w));
        float n010 = corner4(i, f, ivec4(0, 1, 0, w));
        float n110 = corner4(i, f, ivec4(1, 1, 0, w));
        float n001 = corner4(i, f, ivec4(0, 0, 1, w));
        float n101 = corner4(i, f, ivec4(1, 0, 1, w));
        float n011 = corner4(i, f, ivec4(0, 1, 1, w));
        float n111 = corner4(i, f, ivec4(1, 1, 1, w));
        r[w] = mix(mix(mix(n000, n100, u.x), mix(n010, n110, u.x), u.y),
                   mix(mix(n001, n101, u.x), mix(n011, n111, u.x), u.y), u.z);
    }
    return mix(r[0], r[1], u.w) * 1.3;
}

float fbm4(vec4 p, int octaves, float gain) {
    float sum = 0.0, amp = 1.0, norm = 0.0;
    for (int i = 0; i < 8; i++) {
        if (i >= octaves) break;
        sum += amp * gnoise4(p);
        norm += amp;
        amp *= gain;
        p = p * 2.03 + vec4(17.13, -9.71, 5.37, 3.19);
    }
    return norm > 0.0 ? sum / norm : 0.0;
}

void main() {
    vec2 q = uOriginXZ + (floor(gl_FragCoord.xy) + 0.5) * uVS;

    vec2 warp = vec2(fbm(vec3(q / WARP_SCALE, uEvolve * 0.7 + 11.3), 3, 0.5),
                     fbm(vec3(q / WARP_SCALE + 37.1, uEvolve * 0.7 - 5.2), 3, 0.5)) * WARP_STRENGTH;
    float m = fbm(vec3((q + warp) / MAP_SCALE, uEvolve), OCTAVES, GAIN) * 0.5 * CONTRAST + 0.5;
    float region = fbm(vec3(q / REGION_SCALE + 91.7, uEvolve * 0.3 + 3.1), 3, 0.5);
    float cov = m + region * REGION_STRENGTH - (1.0 - COVERAGE);
    if (cov + DETAIL_STRENGTH <= 0.0) { outColor = vec4(0.0); return; }

    float y = (float(uLayer) + 0.5) * uVS;
    float h = clamp(y / (float(uHeight) * uVS), 0.0, 1.0);
    float d = clamp(cov * TOWER, 0.0, 1.0) - pow(h, ROUNDNESS);

    float detail = fbm4(vec4(vec3(q.x, y, q.y) / DETAIL_SCALE, uEvolve * 3.0), DETAIL_OCTAVES, 0.5);
    float baseMask = mix(1.0, smoothstep(0.0, 0.25, h), BASE_FLAT);
    d += detail * DETAIL_STRENGTH * (detail > 0.0 ? baseMask : 1.0);

    outColor = vec4(d > 0.0 ? 1.0 : 0.0);
}
