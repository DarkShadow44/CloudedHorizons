package com.cloudedhorizons.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

import org.apache.commons.io.IOUtils;

import com.cloudedhorizons.Config;

/**
 * Smoke-test GPU cloud renderer. A fragment shader generates the voxel volume into a 3D texture, one layer per draw.
 * The field's time axis advances at {@link #getMorphSpeed()}, and the volume is regenerated every
 * {@link #REGEN_INTERVAL_TICKS} ticks while it changes. Each frame, the volume's bounding box is drawn and a DDA raymarch finds the voxel surface and
 * writes its depth.
 */
public final class CloudRenderer {

    private static final int VOXEL_SIZE = 8;
    private static final int SIZE_X = 256;
    /** Upper limit for the number of voxel layers, to keep the 3D texture and regeneration cost bounded. */
    private static final int MAX_LAYERS = 128;
    private static final int SIZE_Z = 256;
    /** World position of the volume's minimum corner, centred on the world origin. */
    private static final double ORIGIN_X = -SIZE_X * VOXEL_SIZE / 2.0D;
    private static final double ORIGIN_Z = -SIZE_Z * VOXEL_SIZE / 2.0D;
    private static final int SEED = 1;
    /** Regenerate the volume at most this often, in world ticks. */
    private static final double REGEN_INTERVAL_TICKS = 2.0D;

    private static double morphPhase;
    private static double lastWorldTime = Double.NaN;
    private static double lastRegenTime = Double.NEGATIVE_INFINITY;

    /** Number of voxel layers the 3D texture is currently allocated with. */
    private static int allocatedLayers;

    private static boolean initialized;
    private static boolean failed;

    private static int voxelTexture;
    private static int generateProgram;
    private static int volumeProgram;
    private static int emptyVao;
    private static int cubeVao;
    private static int cubeVbo;
    private static int generateFbo;

    private static final FloatBuffer MATRIX = GLAllocation.createDirectFloatBuffer(16);
    private static final FloatBuffer PROJECTION = GLAllocation.createDirectFloatBuffer(16);

    private CloudRenderer() {}

    public static double getMorphSpeed() {
        return Config.morphSpeed;
    }

    public static void setMorphSpeed(double speed) {
        Config.morphSpeed = speed;
        Config.saveClouds();
    }

    public static double getBaseY() {
        return Config.cloudHeight;
    }

    public static void setBaseY(double y) {
        // Only affects placement, so changing it needs no regeneration.
        Config.cloudHeight = y;
        Config.saveClouds();
    }

    /** Height of the cloud field in voxels. */
    public static int getFieldHeight() {
        return Config.fieldHeight;
    }

    public static void setFieldHeight(int voxels) {
        // The texture is reallocated and regenerated on the next frame (see render).
        Config.fieldHeight = voxels;
        Config.saveClouds();
    }

    /** Voxel layers to allocate: the configured field height, limited to 1..MAX_LAYERS. */
    private static int layers() {
        return Math.max(1, Math.min(MAX_LAYERS, Config.fieldHeight));
    }

    public static void render(Minecraft mc, World world, float partialTicks) {
        if (failed) {
            return;
        }
        if (!initialized) {
            try {
                initialize(mc);
            } catch (RuntimeException e) {
                failed = true;
                System.err.println("[Clouded Horizons] GPU cloud initialisation failed, clouds disabled: " + e);
                e.printStackTrace();
                return;
            }
        }
        if (layers() != allocatedLayers) {
            allocateVoxelTexture();
            generate(mc);
        }
        updateMorph(mc, world, partialTicks);

        GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, MATRIX);
        GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, PROJECTION);

        Vec3 colour = world.getCloudColour(partialTicks);
        double sunAngle = world.getCelestialAngle(partialTicks) * Math.PI * 2.0D;

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        // Draw back faces so the box still rasterises when the camera is inside it.
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_FRONT);
        // The box can extend past the far plane; clamp instead of clipping it away.
        GL11.glEnable(GL32.GL_DEPTH_CLAMP);

        GL20.glUseProgram(volumeProgram);
        GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(volumeProgram, "uProj"), false, PROJECTION);
        GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(volumeProgram, "uView"), false, MATRIX);
        GL20.glUniform3f(
                GL20.glGetUniformLocation(volumeProgram, "uBoxMin"),
                (float) (ORIGIN_X - RenderManager.renderPosX),
                (float) (Config.cloudHeight - RenderManager.renderPosY),
                (float) (ORIGIN_Z - RenderManager.renderPosZ));
        GL20.glUniform3f(
                GL20.glGetUniformLocation(volumeProgram, "uBoxSize"),
                SIZE_X * VOXEL_SIZE,
                allocatedLayers * VOXEL_SIZE,
                SIZE_Z * VOXEL_SIZE);
        GL20.glUniform3f(GL20.glGetUniformLocation(volumeProgram, "uDims"), SIZE_X, allocatedLayers, SIZE_Z);
        GL20.glUniform1f(GL20.glGetUniformLocation(volumeProgram, "uVS"), VOXEL_SIZE);
        GL20.glUniform3f(
                GL20.glGetUniformLocation(volumeProgram, "uColour"),
                (float) colour.xCoord,
                (float) colour.yCoord,
                (float) colour.zCoord);
        GL20.glUniform3f(
                GL20.glGetUniformLocation(volumeProgram, "uSunDir"),
                (float) -Math.sin(sunAngle),
                (float) Math.cos(sunAngle),
                0.0F);
        GL20.glUniform1i(GL20.glGetUniformLocation(volumeProgram, "uVoxels"), 0);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL12.GL_TEXTURE_3D, voxelTexture);
        GL30.glBindVertexArray(cubeVao);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 36);
        GL30.glBindVertexArray(0);
        GL11.glBindTexture(GL12.GL_TEXTURE_3D, 0);
        GL20.glUseProgram(0);

        GL11.glDisable(GL32.GL_DEPTH_CLAMP);
        GL11.glCullFace(GL11.GL_BACK);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void initialize(Minecraft mc) {
        initialized = true;
        generateProgram = createProgram("fullscreen.vsh", "generate.fsh", "noise.fsh");
        volumeProgram = createProgram("volume.vsh", "volume.fsh");
        emptyVao = GL30.glGenVertexArrays();
        createCube();
        createVoxelTexture();
        allocateVoxelTexture();
        generateFbo = GL30.glGenFramebuffers();
        generate(mc);
    }

    /**
     * Advances the morph phase by the game time elapsed since the last frame, so it stops while the game is paused
     * and a speed change does not make it jump.
     */
    private static void updateMorph(Minecraft mc, World world, float partialTicks) {
        double now = world.getTotalWorldTime() + partialTicks;
        if (Double.isNaN(lastWorldTime) || now < lastWorldTime || now - lastWorldTime > 100.0D) {
            // First frame, world change, or a large time skip: do not advance.
            lastWorldTime = now;
            return;
        }
        morphPhase += (now - lastWorldTime) / 20.0D * Config.morphSpeed;
        lastWorldTime = now;
        if (Config.morphSpeed != 0.0D && now - lastRegenTime >= REGEN_INTERVAL_TICKS) {
            lastRegenTime = now;
            generate(mc);
        }
    }

    private static void createVoxelTexture() {
        voxelTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL12.GL_TEXTURE_3D, voxelTexture);
        GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL12.GL_TEXTURE_WRAP_R, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        GL11.glBindTexture(GL12.GL_TEXTURE_3D, 0);
    }

    /** (Re)allocates the voxel texture storage for the configured field height. */
    private static void allocateVoxelTexture() {
        allocatedLayers = layers();
        GL11.glBindTexture(GL12.GL_TEXTURE_3D, voxelTexture);
        // Layout (x, z, y): each generation draw fills one horizontal layer.
        GL12.glTexImage3D(
                GL12.GL_TEXTURE_3D,
                0,
                GL30.GL_R8,
                SIZE_X,
                SIZE_Z,
                allocatedLayers,
                0,
                GL11.GL_RED,
                GL11.GL_UNSIGNED_BYTE,
                (ByteBuffer) null);
        GL11.glBindTexture(GL12.GL_TEXTURE_3D, 0);
    }

    /** Runs the generation shader once per voxel layer into the 3D texture, then restores Minecraft's GL state. */
    private static void generate(Minecraft mc) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, generateFbo);
        GL11.glViewport(0, 0, SIZE_X, SIZE_Z);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_CULL_FACE);

        GL20.glUseProgram(generateProgram);
        GL20.glUniform1f(GL20.glGetUniformLocation(generateProgram, "uVS"), VOXEL_SIZE);
        GL20.glUniform2f(
                GL20.glGetUniformLocation(generateProgram, "uOriginXZ"),
                (float) ORIGIN_X,
                (float) ORIGIN_Z);
        GL30.glUniform1ui(GL20.glGetUniformLocation(generateProgram, "uSeed"), SEED);
        // Wrapped so float precision in the shader stays good over long sessions; the jump is rare and slow.
        GL20.glUniform1f(GL20.glGetUniformLocation(generateProgram, "uEvolve"), (float) (morphPhase % 1000.0D));
        int layerLocation = GL20.glGetUniformLocation(generateProgram, "uLayer");

        GL30.glBindVertexArray(emptyVao);
        for (int layer = 0; layer < allocatedLayers; layer++) {
            GL30.glFramebufferTextureLayer(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, voxelTexture, 0, layer);
            if (layer == 0) {
                int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
                if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
                    throw new IllegalStateException("Voxel framebuffer incomplete: 0x" + Integer.toHexString(status));
                }
            }
            GL20.glUniform1i(layerLocation, layer);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
        }
        GL30.glBindVertexArray(0);
        GL20.glUseProgram(0);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        if (OpenGlHelper.isFramebufferEnabled()) {
            mc.getFramebuffer().bindFramebuffer(true);
        } else {
            GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
        }
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

    /** Unit cube as 12 triangles, counter-clockwise when seen from outside. */
    private static void createCube() {
        float[][] quads = {
                { 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0 }, // +Y
                { 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1 }, // -Y
                { 1, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1 }, // +X
                { 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0 }, // -X
                { 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1 }, // +Z
                { 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0 }, // -Z
        };
        FloatBuffer data = GLAllocation.createDirectFloatBuffer(36 * 3);
        for (float[] q : quads) {
            for (int corner : new int[] { 0, 1, 2, 0, 2, 3 }) {
                data.put(q, corner * 3, 3);
            }
        }
        data.flip();

        cubeVao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(cubeVao);
        cubeVbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, cubeVbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 12, 0L);
        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    /**
     * Links a program from one vertex shader and one or more fragment shader objects. Extra fragment shaders hold
     * shared functions (e.g. noise.fsh) that the main one declares as prototypes.
     */
    private static int createProgram(String vertexName, String... fragmentNames) {
        int program = GL20.glCreateProgram();
        int[] shaders = new int[fragmentNames.length + 1];
        shaders[0] = compileShader(GL20.GL_VERTEX_SHADER, vertexName);
        for (int i = 0; i < fragmentNames.length; i++) {
            shaders[i + 1] = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentNames[i]);
        }
        for (int shader : shaders) {
            GL20.glAttachShader(program, shader);
        }
        GL20.glLinkProgram(program);
        for (int shader : shaders) {
            GL20.glDeleteShader(shader);
        }
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new IllegalStateException(
                    "Link failed (" + vertexName + ", " + String.join(", ", fragmentNames) + "): "
                            + GL20.glGetProgramInfoLog(program, 8192));
        }
        return program;
    }

    private static int compileShader(int type, String name) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, readShader(name));
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new IllegalStateException("Compile failed (" + name + "): " + GL20.glGetShaderInfoLog(shader, 8192));
        }
        return shader;
    }

    private static String readShader(String name) {
        String path = "/assets/cloudedhorizons/shaders/" + name;
        try (InputStream in = CloudRenderer.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing shader " + path);
            }
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read shader " + path, e);
        }
    }
}
