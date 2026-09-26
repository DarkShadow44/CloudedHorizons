#version 330 core
// Writes one horizontal layer (y) of the voxel volume as opacity: 0 = empty, 1 = opaque, between = translucent.
// The field value is the sum of up to MAX_NOISE_LAYERS layers of 4D simplex noise, each with its own scale,
// multiplier and offset; a voxel is opaque where the sum is above the global cutoff, and a translucent shell whose
// opacity falls to 0 over uSoftness below the cutoff rounds off the cloud surfaces. Linked together with noise.fsh.
// uEvolve moves the noise along its time axis, which morphs the clouds in place.

uniform int uLayer;
uniform int uLayers;      // voxel layers in the field
uniform float uVS;        // voxel size in blocks
uniform vec2 uOriginXZ;   // world X/Z of the volume's minimum corner
uniform float uEvolve;
uniform float uCutoff;    // voxel is solid where the summed layers are above this

const int MAX_NOISE_LAYERS = 8; // must match Config.MAX_NOISE_LAYERS
uniform int uNoiseLayers;
uniform vec3 uLayerScale[MAX_NOISE_LAYERS];     // noise feature size per axis (x, y, z), in voxels
uniform vec2 uLayerMulOffset[MAX_NOISE_LAYERS]; // layer value = noise * x + y
uniform float uSoftness;  // summed-noise width below the threshold over which voxels fade out; 0 = hard cut
uniform float uEdgeFade;  // voxels from top and bottom over which clouds thin out; 0 = hard cut

out vec4 outColor;


float snoise4(vec4 v); // noise.fsh

void main() {
    // Voxel coordinates (x, y, z); the volume origin is a whole number of voxels.
    vec2 xz = uOriginXZ / uVS + floor(gl_FragCoord.xy) + 0.5;
    vec3 v = vec3(xz.x, float(uLayer) + 0.5, xz.y);
    float n = 0.0;
    float peak = 0.0; // about the largest value the sum reaches, for the edge fade
    for (int i = 0; i < MAX_NOISE_LAYERS; i++) {
        if (i >= uNoiseLayers) break;
        // Each layer samples a different, far-away region of the noise so layers with equal scales still differ.
        vec4 shift = float(i) * vec4(1731.7, 911.3, 2179.1, 537.9);
        vec2 mo = uLayerMulOffset[i];
        n += snoise4(vec4(v / max(uLayerScale[i], vec3(1e-3)), uEvolve) + shift) * mo.x + mo.y;
        peak += 0.97 * abs(mo.x) + mo.y;
    }

    // The noise is scaled to [-1, 1], but a single layer is bell-shaped (std 0.28, 99.9% within 0.86, sampled extremes
    // ~0.97), so a ramp to far above that removes almost all clouds early. End the ramp at 0.97, the sampled peak.
    // Measure from layer centers so both outermost layers are empty instead of leaving a thin clipped cap.
    float threshold = uCutoff;
    bool fadeEnabled = uEdgeFade >= 1.0 && uLayers >= 3;
    if (fadeEnabled) {
        float edgeLayer = min(float(uLayer), float(uLayers - 1 - uLayer));
        float fadeLayers = min(uEdgeFade, ceil(float(uLayers) * 0.5));
        float fade = clamp(edgeLayer / max(fadeLayers - 1.0, 1.0), 0.0, 1.0);
        threshold = mix(max(peak, uCutoff), uCutoff, fade);
    }
    bool atBoundary = fadeEnabled && (uLayer == 0 || uLayer == uLayers - 1);
    float alpha = 0.0;
    if (!atBoundary) {
        if (n > threshold) {
            alpha = 1.0;
        } else if (uSoftness > 0.0 && n > threshold - uSoftness) {
            // Keep every shell voxel at least 1/255 so the R8 texture does not round thin edges away.
            alpha = max((n - (threshold - uSoftness)) / uSoftness, 1.0 / 255.0);
        }
    }
    outColor = vec4(alpha);
}
