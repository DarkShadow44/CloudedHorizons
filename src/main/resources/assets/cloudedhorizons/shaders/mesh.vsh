#version 430 core
// Builds the cloud quads directly from the face buffer written by mesh.comp; there are no vertex attributes.
// Each face is 6 vertices (two triangles), and gl_VertexID (which includes the draw's first vertex) selects the
// face and the corner.

layout(std430, binding = 0) readonly buffer Faces { uvec2 faces[]; };

uniform mat4 uProj;
uniform mat4 uView;
uniform vec3 uBoxMin; // field minimum corner relative to the render origin
uniform float uVS;    // voxel size in blocks

flat out vec3 vNormal;

// Per direction (+X, -X, +Y, -Y, +Z, -Z): normal and the two edge vectors of the face, ordered so that
// cross(U, W) = N, which makes the quad counter-clockwise when seen from outside.
const vec3 N[6] = vec3[6](vec3(1, 0, 0), vec3(-1, 0, 0), vec3(0, 1, 0), vec3(0, -1, 0), vec3(0, 0, 1), vec3(0, 0, -1));
const vec3 U[6] = vec3[6](vec3(0, 1, 0), vec3(0, 0, 1), vec3(0, 0, 1), vec3(1, 0, 0), vec3(1, 0, 0), vec3(0, 1, 0));
const vec3 W[6] = vec3[6](vec3(0, 0, 1), vec3(0, 1, 0), vec3(1, 0, 0), vec3(0, 0, 1), vec3(0, 1, 0), vec3(1, 0, 0));
// Corners of the two triangles (0, 1, 2) and (0, 2, 3) as (u, w).
const vec2 CORNERS[6] = vec2[6](vec2(0, 0), vec2(1, 0), vec2(1, 1), vec2(0, 0), vec2(1, 1), vec2(0, 1));

void main() {
    uint vertex = uint(gl_VertexID);
    uvec2 face = faces[vertex / 6u];
    vec3 voxel = vec3(float(face.x & 0xfffu), float(face.x >> 24), float((face.x >> 12) & 0xfffu));
    uint d = face.y & 7u;

    vec2 corner = CORNERS[vertex % 6u];
    // Faces on the positive side lie on the voxel's far plane.
    vec3 p = voxel + max(N[d], vec3(0.0)) + U[d] * corner.x + W[d] * corner.y;

    vNormal = N[d];
    gl_Position = uProj * uView * vec4(uBoxMin + p * uVS, 1.0);
}
