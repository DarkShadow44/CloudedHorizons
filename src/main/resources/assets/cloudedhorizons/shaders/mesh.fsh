#version 330 core
// One ambient material for every face, including opaque/translucent interfaces.
// Height is perspective-correct interpolated, so adjacent faces agree along their shared edges.
// A restrained sky-light ramp shades the lower cloud layer without dark cube sides or opacity switches.
uniform vec3 uColour;
#ifdef TRANSLUCENT
uniform sampler2D uSceneDepth; // world + opaque cloud depth, copied before the translucent pass
#endif

in float vHeight;
flat in float vAlpha;
layout(location = 0) out vec4 outColor;

void main() {
    float light = mix(0.88, 1.0, smoothstep(0.0, 1.0, vHeight));
    vec3 colour = uColour * light;
#ifndef TRANSLUCENT
    outColor = vec4(colour, 1.0);
#else

    // The accumulation FBO has no depth attachment. Compare against the captured destination depth;
    // this also avoids assuming Minecraft/Angelica's framebuffer or depth attachment format.
    if (gl_FragCoord.z > texelFetch(uSceneDepth, ivec2(gl_FragCoord.xy), 0).r) discard;

    // Weighted blended transparency, packed into one additive target.
    // Bounded, opacity-only weights favour dense voxels without depending on the extended far plane.
    float weight = 0.01 + vAlpha * vAlpha;
    // All clouds have the same RGB tint, so accumulate scalar lighting and apply the tint at resolve.
    // -log(1-alpha) turns the transmittance product into a sum; only alpha < 1 reaches this pass.
    outColor = vec4(light * vAlpha * weight, vAlpha * weight, -log(1.0 - vAlpha), 0.0);
#endif
}
