#version 330 core
// Voxel DDA through the generated volume. Writes real depth so terrain and clouds occlude each other correctly.
// Empty coarse cells (see coarse.fsh) are skipped in one step.

uniform sampler3D uVoxels; // layout (x, z, y)
uniform sampler3D uCoarse; // layout (x, z, y), one texel per COARSE block of voxels
uniform mat4 uProj;
uniform mat4 uView;
uniform vec3 uBoxMin;
uniform vec3 uDims;        // voxel counts (x, y, z)
uniform float uVS;
uniform vec3 uColour;
uniform vec3 uSunDir;

in vec3 vPos;
flat in vec3 vEye;
out vec4 outColor;

const vec3 COARSE = vec3(8.0, 4.0, 8.0); // must match coarse.fsh and CloudRenderer
const int MAX_STEPS = 2048;


void main() {
    vec3 rd = normalize(vPos - vEye);
    rd = mix(rd, vec3(1e-6), vec3(lessThan(abs(rd), vec3(1e-6))));
    vec3 ro = (vEye - uBoxMin) / uVS;
    vec3 inv = 1.0 / rd;
    vec3 t1 = -ro * inv, t2 = (uDims - ro) * inv;
    vec3 tmn = min(t1, t2), tmx = max(t1, t2);
    float tn = max(max(tmn.x, tmn.y), tmn.z);
    float tf = min(min(tmx.x, tmx.y), tmx.z);
    if (tf < max(tn, 0.0)) discard;

    vec3 nrm = tn > 0.0
        ? (tn == tmn.x ? vec3(-sign(rd.x), 0, 0) : tn == tmn.y ? vec3(0, -sign(rd.y), 0) : vec3(0, 0, -sign(rd.z)))
        : vec3(0.0, 1.0, 0.0);
    float t = max(tn, 0.0) + 1e-3;
    vec3 sg = step(0.0, rd);
    bool hit = false;
    ivec3 hiCell = ivec3(uDims) - 1;
    for (int i = 0; i < MAX_STEPS; i++) {
        if (t > tf) break;
        ivec3 c = clamp(ivec3(floor(ro + rd * t)), ivec3(0), hiCell);
        ivec3 cc = ivec3(vec3(c) / COARSE);
        vec3 lo, hi;
        if (texelFetch(uCoarse, cc.xzy, 0).r < 0.5) {
            // Empty coarse cell: jump to its far side.
            lo = vec3(cc) * COARSE;
            hi = lo + COARSE;
        } else {
            if (texelFetch(uVoxels, c.xzy, 0).r > 0.5) { hit = true; break; }
            lo = vec3(c);
            hi = lo + 1.0;
        }
        vec3 tb = (mix(lo, hi, sg) - ro) * inv;
        float te = min(min(tb.x, tb.y), tb.z);
        nrm = te == tb.x ? vec3(-sign(rd.x), 0, 0) : te == tb.y ? vec3(0, -sign(rd.y), 0) : vec3(0, 0, -sign(rd.z));
        t = te + 1e-3;
    }
    if (!hit) discard;

    vec3 p = uBoxMin + (ro + rd * t) * uVS;
    vec4 clip = uProj * uView * vec4(p, 1.0);
    gl_FragDepth = min(clip.z / clip.w * 0.5 + 0.5, 1.0);

    float face = nrm.y > 0.5 ? 1.0 : nrm.y < -0.5 ? 0.7 : abs(nrm.x) > 0.5 ? 0.9 : 0.8;
    float sun = 0.85 + 0.15 * max(dot(nrm, uSunDir), 0.0);
    outColor = vec4(uColour * face * sun, 1.0);
}
