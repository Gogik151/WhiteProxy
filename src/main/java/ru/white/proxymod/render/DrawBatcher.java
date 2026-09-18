package ru.white.proxymod.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public final class DrawBatcher {

    public interface Batched {
        default int batchLayer() {
            return 0;
        }

        void uploadBatch(CommandEncoder encoder);

        void drawBatch(RenderPass pass);

        void discardBatch();
    }

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f(0, 0, 0);
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    private static final ArrayList<Batched> active = new ArrayList<>(8);
    private static boolean enabled = false;

    private DrawBatcher() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        if (!value) {
            flushPending();
        }
        enabled = value;
    }

    public static void register(Batched owner) {
        if (!active.contains(owner)) {
            active.add(owner);
        }
    }

    public static void flushPending() {
        if (active.isEmpty()) return;
        Batched[] pending = active.toArray(new Batched[0]);
        active.clear();
        Arrays.sort(pending, Comparator.comparingInt(Batched::batchLayer));

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getFramebuffer() == null) {
            for (Batched batch : pending) {
                batch.discardBatch();
            }
            return;
        }

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        for (Batched batch : pending) {
            batch.uploadBatch(encoder);
        }

        GpuBufferSlice dynamicTransforms = writeDynamicTransforms();

        try (RenderPass pass = encoder.createRenderPass(
                () -> "proxymod:2d_batch",
                client.getFramebuffer().getColorAttachmentView(),
                OptionalInt.empty(),
                client.getFramebuffer().getDepthAttachmentView(),
                OptionalDouble.empty())) {

            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamicTransforms);
            for (Batched batch : pending) {
                batch.drawBatch(pass);
            }
        } finally {
            for (Batched batch : pending) {
                batch.discardBatch();
            }
        }
    }

    public static void drawImmediate(Batched owner) {
        drawImmediate(owner, true);
    }

    public static void drawImmediate(Batched owner, boolean upload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getFramebuffer() == null) {
            owner.discardBatch();
            return;
        }

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        if (upload) {
            owner.uploadBatch(encoder);
        }

        GpuBufferSlice dynamicTransforms = writeDynamicTransforms();

        try (RenderPass pass = encoder.createRenderPass(
                () -> "proxymod:2d_immediate",
                client.getFramebuffer().getColorAttachmentView(),
                OptionalInt.empty(),
                client.getFramebuffer().getDepthAttachmentView(),
                OptionalDouble.empty())) {

            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamicTransforms);
            owner.drawBatch(pass);
        } finally {
            owner.discardBatch();
        }
    }

    private static GpuBufferSlice writeDynamicTransforms() {
        return RenderSystem.getDynamicUniforms()
                .write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
    }
}
