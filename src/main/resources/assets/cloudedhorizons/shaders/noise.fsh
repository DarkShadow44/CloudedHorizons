#version 330 core
// Shared noise functions. Compiled as a separate fragment shader object and linked into the programs that
// declare the prototypes below. Same noise as webdemo/index.html (GLSL_NOISE).
//
//   float snoise4(vec4 v);

uniform uint uSeed;

uvec4 pcg4d(uvec4 v) {
    v = v * 1664525u + 1013904223u;
    v.x += v.y * v.w; v.y += v.z * v.x; v.z += v.x * v.y; v.w += v.y * v.z;
    v ^= v >> 16u;
    v.x += v.y * v.w; v.y += v.z * v.x; v.z += v.x * v.y; v.w += v.y * v.z;
    return v;
}

// Pseudo-random unit gradient for a 4D lattice point.
vec4 grad4(vec4 p) {
    uvec4 h = pcg4d(uvec4(ivec4(p)) + uvec4(uSeed * 7919u, uSeed * 104729u, uSeed * 15485863u, uSeed * 32452843u));
    vec4 g = vec4(h & 0xffffu) * (2.0 / 65535.0) - 1.0;
    return g * inversesqrt(max(dot(g, g), 1e-6));
}

// 4D simplex noise in roughly [-1, 1]. The fourth axis is time: moving along it changes the 3D field in place
// without translating it in space.
float snoise4(vec4 v) {
    const float F4 = 0.30901699437; // (sqrt(5) - 1) / 4
    const float G4 = 0.13819660113; // (5 - sqrt(5)) / 20
    vec4 s = floor(v + dot(v, vec4(F4)));
    vec4 x0 = v - s + dot(s, vec4(G4));

    // Rank the components of x0; the simplex corners are visited in descending order.
    vec4 rank = vec4(0.0);
    vec3 gx = step(x0.yzw, x0.xxx);
    rank.x += gx.x + gx.y + gx.z;
    rank.yzw += 1.0 - gx;
    vec3 gyz = step(x0.zww, x0.yyz);
    rank.y += gyz.x + gyz.y;
    rank.zw += 1.0 - gyz.xy;
    rank.z += gyz.z;
    rank.w += 1.0 - gyz.z;
    vec4 i1 = clamp(rank - 2.0, 0.0, 1.0);
    vec4 i2 = clamp(rank - 1.0, 0.0, 1.0);
    vec4 i3 = clamp(rank, 0.0, 1.0);

    vec4 x1 = x0 - i1 + G4;
    vec4 x2 = x0 - i2 + 2.0 * G4;
    vec4 x3 = x0 - i3 + 3.0 * G4;
    vec4 x4 = x0 - 1.0 + 4.0 * G4;

    vec4 o[5] = vec4[5](vec4(0.0), i1, i2, i3, vec4(1.0));
    vec4 x[5] = vec4[5](x0, x1, x2, x3, x4);
    float n = 0.0;
    for (int k = 0; k < 5; k++) {
        float t = 0.6 - dot(x[k], x[k]);
        if (t > 0.0) { t *= t; n += t * t * dot(grad4(s + o[k]), x[k]); }
    }
    return n * 27.0;
}
