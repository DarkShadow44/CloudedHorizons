package com.cloudedhorizons;

/**
 * One noise layer of the cloud field. The field value is the sum over all layers of
 * {@code snoise4(voxel / scale, time) * multiplier + offset}; the global cutoff is applied to that sum.
 */
public final class NoiseLayer {

    /** Noise feature size along each axis, in voxels. */
    public double scaleX = 32.0D;
    public double scaleY = 32.0D;
    public double scaleZ = 32.0D;
    public double multiplier = 1.0D;
    public double offset = 0.0D;

    public NoiseLayer copy() {
        NoiseLayer c = new NoiseLayer();
        c.scaleX = scaleX;
        c.scaleY = scaleY;
        c.scaleZ = scaleZ;
        c.multiplier = multiplier;
        c.offset = offset;
        return c;
    }
}
