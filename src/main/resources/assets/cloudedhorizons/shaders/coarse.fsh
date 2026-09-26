#version 330 core
// Writes one layer of the coarse occupancy grid: 1 if any voxel in the COARSE block is solid, else 0.
// The volume shader uses it to skip empty space. Same idea as webdemo/index.html (FS_COARSE).

uniform sampler3D uVoxels; // layout (x, z, y)
uniform int uLayer;        // coarse layer (y)
uniform ivec3 uDims;       // voxel counts (x, y, z)

out vec4 outColor;

const ivec3 COARSE = ivec3(8, 4, 8); // voxels per coarse cell (x, y, z); must match CloudRenderer and volume.fsh

void main() {
    ivec3 base = ivec3(ivec2(gl_FragCoord.xy), uLayer) * COARSE.xzy; // (x, z, y) layout
    ivec3 hi = min(base + COARSE.xzy, uDims.xzy);
    float occ = 0.0;
    for (int y = base.z; y < hi.z; y++)
    for (int z = base.y; z < hi.y; z++)
    for (int x = base.x; x < hi.x; x++) {
        occ = max(occ, texelFetch(uVoxels, ivec3(x, z, y), 0).r);
    }
    outColor = vec4(occ);
}
