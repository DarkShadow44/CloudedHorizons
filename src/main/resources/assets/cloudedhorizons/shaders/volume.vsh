#version 330 core
// Draws the volume's bounding box. Positions are relative to the render origin (the interpolated camera entity).
layout(location = 0) in vec3 aPos; // unit cube corner, 0..1

uniform mat4 uProj;
uniform mat4 uView;
uniform vec3 uBoxMin;
uniform vec3 uBoxSize;

out vec3 vPos;
flat out vec3 vEye;

void main() {
    vPos = uBoxMin + aPos * uBoxSize;
    // The eye is not always at the render origin (view bobbing, third person), so take it from the view matrix.
    vEye = inverse(uView)[3].xyz;
    gl_Position = uProj * uView * vec4(vPos, 1.0);
}
