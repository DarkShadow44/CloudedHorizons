#version 330 core
// Flat face shading for the meshed clouds.

uniform vec3 uColour;
uniform vec3 uSunDir;

flat in vec3 vNormal;
out vec4 outColor;

void main() {
    float face = vNormal.y > 0.5 ? 1.0 : vNormal.y < -0.5 ? 0.7 : abs(vNormal.x) > 0.5 ? 0.9 : 0.8;
    float sun = 0.85 + 0.15 * max(dot(vNormal, uSunDir), 0.0);
    outColor = vec4(uColour * face * sun, 1.0);
}
