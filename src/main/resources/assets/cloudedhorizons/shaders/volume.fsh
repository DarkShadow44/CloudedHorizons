#version 330 core
// Voxel DDA through the generated volume. Writes real depth so terrain and clouds occlude each other correctly.

uniform sampler3D uVoxels; // layout (x, z, y)
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

bool solid(ivec3 c) {
    if (any(lessThan(c, ivec3(0))) || any(greaterThanEqual(c, ivec3(uDims)))) return false;
    return texelFetch(uVoxels, c.xzy, 0).r > 0.5;
}

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
    for (int i = 0; i < 1024; i++) {
        if (t > tf) break;
        ivec3 c = ivec3(floor(ro + rd * t));
        if (solid(c)) { hit = true; break; }
        vec3 tb = (mix(vec3(c), vec3(c) + 1.0, sg) - ro) * inv;
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
