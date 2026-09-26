package com.cloudedhorizons.client;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

/**
 * Smoke-test cloud renderer: draws a static, hand-built voxel cloud field around the world origin. Only exposed faces
 * are emitted. Face brightness stands in for lighting.
 */
public final class CloudRenderer {

    private static final int VOXEL_SIZE = 8;
    private static final int SIZE_X = 96;
    private static final int SIZE_Y = 8;
    private static final int SIZE_Z = 96;
    private static final double BASE_Y = 160.0D;
    /** World position of the grid's minimum corner, centred on the world origin. */
    private static final double ORIGIN_X = -SIZE_X * VOXEL_SIZE / 2.0D;
    private static final double ORIGIN_Z = -SIZE_Z * VOXEL_SIZE / 2.0D;

    private static final boolean[] VOXELS = buildField();

    private CloudRenderer() {}

    /** Places flat-based, dome-topped blobs at seeded random positions. */
    private static boolean[] buildField() {
        boolean[] voxels = new boolean[SIZE_X * SIZE_Y * SIZE_Z];
        Random random = new Random(1234L);
        for (int blob = 0; blob < 40; blob++) {
            double cx = random.nextDouble() * SIZE_X;
            double cz = random.nextDouble() * SIZE_Z;
            double radius = 3.0D + random.nextDouble() * 7.0D;
            double height = 1.0D + random.nextDouble() * (SIZE_Y - 1);
            for (int x = 0; x < SIZE_X; x++) {
                for (int z = 0; z < SIZE_Z; z++) {
                    double dx = (x + 0.5D - cx) / radius;
                    double dz = (z + 0.5D - cz) / radius;
                    double r2 = dx * dx + dz * dz;
                    if (r2 >= 1.0D) {
                        continue;
                    }
                    int top = (int) Math.ceil(height * Math.sqrt(1.0D - r2));
                    for (int y = 0; y < Math.min(top, SIZE_Y); y++) {
                        voxels[index(x, y, z)] = true;
                    }
                }
            }
        }
        return voxels;
    }

    private static int index(int x, int y, int z) {
        return (y * SIZE_Z + z) * SIZE_X + x;
    }

    private static boolean solid(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= SIZE_X || y >= SIZE_Y || z >= SIZE_Z) {
            return false;
        }
        return VOXELS[index(x, y, z)];
    }

    public static void render(Minecraft mc, World world, float partialTicks) {
        EntityLivingBase view = mc.renderViewEntity;
        if (view == null) {
            return;
        }
        double camX = view.lastTickPosX + (view.posX - view.lastTickPosX) * partialTicks;
        double camY = view.lastTickPosY + (view.posY - view.lastTickPosY) * partialTicks;
        double camZ = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partialTicks;

        Vec3 cloudColour = world.getCloudColour(partialTicks);
        float r = (float) cloudColour.xCoord;
        float g = (float) cloudColour.yCoord;
        float b = (float) cloudColour.zCoord;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        // Relative to the camera so float precision stays good far from the origin.
        tessellator.setTranslation(ORIGIN_X - camX, BASE_Y - camY, ORIGIN_Z - camZ);
        for (int y = 0; y < SIZE_Y; y++) {
            for (int z = 0; z < SIZE_Z; z++) {
                for (int x = 0; x < SIZE_X; x++) {
                    if (solid(x, y, z)) {
                        emitVoxel(tessellator, x, y, z, r, g, b);
                    }
                }
            }
        }
        tessellator.draw();
        tessellator.setTranslation(0.0D, 0.0D, 0.0D);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Emits the exposed faces of one voxel with counter-clockwise front faces. */
    private static void emitVoxel(Tessellator t, int x, int y, int z, float r, float g, float b) {
        double x0 = x * VOXEL_SIZE, y0 = y * VOXEL_SIZE, z0 = z * VOXEL_SIZE;
        double x1 = x0 + VOXEL_SIZE, y1 = y0 + VOXEL_SIZE, z1 = z0 + VOXEL_SIZE;

        if (!solid(x, y + 1, z)) {
            t.setColorOpaque_F(r, g, b);
            t.addVertex(x0, y1, z0);
            t.addVertex(x0, y1, z1);
            t.addVertex(x1, y1, z1);
            t.addVertex(x1, y1, z0);
        }
        if (!solid(x, y - 1, z)) {
            t.setColorOpaque_F(r * 0.7F, g * 0.7F, b * 0.7F);
            t.addVertex(x0, y0, z0);
            t.addVertex(x1, y0, z0);
            t.addVertex(x1, y0, z1);
            t.addVertex(x0, y0, z1);
        }
        if (!solid(x + 1, y, z)) {
            t.setColorOpaque_F(r * 0.9F, g * 0.9F, b * 0.9F);
            t.addVertex(x1, y0, z0);
            t.addVertex(x1, y1, z0);
            t.addVertex(x1, y1, z1);
            t.addVertex(x1, y0, z1);
        }
        if (!solid(x - 1, y, z)) {
            t.setColorOpaque_F(r * 0.9F, g * 0.9F, b * 0.9F);
            t.addVertex(x0, y0, z0);
            t.addVertex(x0, y0, z1);
            t.addVertex(x0, y1, z1);
            t.addVertex(x0, y1, z0);
        }
        if (!solid(x, y, z + 1)) {
            t.setColorOpaque_F(r * 0.8F, g * 0.8F, b * 0.8F);
            t.addVertex(x0, y0, z1);
            t.addVertex(x1, y0, z1);
            t.addVertex(x1, y1, z1);
            t.addVertex(x0, y1, z1);
        }
        if (!solid(x, y, z - 1)) {
            t.setColorOpaque_F(r * 0.8F, g * 0.8F, b * 0.8F);
            t.addVertex(x0, y0, z0);
            t.addVertex(x0, y1, z0);
            t.addVertex(x1, y1, z0);
            t.addVertex(x1, y0, z0);
        }
    }
}
