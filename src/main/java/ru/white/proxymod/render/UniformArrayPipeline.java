package ru.white.proxymod.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public abstract class UniformArrayPipeline implements DrawBatcher.Batched {

    protected static final float FIXED_GUI_SCALE = 2.0f;
    private static final int HEADER_SIZE = 16;

    private final String debugName;
    private final RenderPipeline renderPipeline;
    private final String uniformName;
    private final int maxItems;
    private final int bufferSize;
    private final int uniformRing;

    private GpuBuffer[] uniformBuffers;
    private int uniformRingIndex = 0;
    private GpuBuffer dummyVertexBuffer;
    private ByteBuffer dataBuffer;
    private boolean initialized = false;

    private record Chunk(GpuBuffer buffer, int count) {}

    private int batchedItems = 0;
    private final ArrayList<Chunk> chunks = new ArrayList<>();

    protected UniformArrayPipeline(String debugName, RenderPipeline renderPipeline, String uniformName,
                                   int maxItems, int itemSize, int uniformRing) {
        this.debugName = debugName;
        this.renderPipeline = renderPipeline;
        this.uniformName = uniformName;
        this.maxItems = maxItems;
        this.bufferSize = HEADER_SIZE + maxItems * itemSize;
        this.uniformRing = uniformRing;
    }

    private void ensureInitialized() {
        if (initialized) return;

        this.dataBuffer = MemoryUtil.memAlloc(bufferSize);

        ByteBuffer dummyData = MemoryUtil.memAlloc(4);
        dummyData.putInt(0);
        dummyData.flip();
        this.dummyVertexBuffer = RenderSystem.getDevice().createBuffer(
                () -> "proxymod:" + debugName + "_dummy_vertex",
                GpuBuffer.USAGE_VERTEX,
                dummyData
        );
        MemoryUtil.memFree(dummyData);

        this.uniformBuffers = new GpuBuffer[uniformRing];
        initialized = true;
    }

    private GpuBuffer nextUniformBuffer() {
        GpuBuffer buf = uniformBuffers[uniformRingIndex];
        if (buf == null) {
            final int idx = uniformRingIndex;
            buf = RenderSystem.getDevice().createBuffer(
                    () -> "proxymod:" + debugName + "_uniform_" + idx,
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    bufferSize
            );
            uniformBuffers[uniformRingIndex] = buf;
        }
        uniformRingIndex = (uniformRingIndex + 1) % uniformRing;
        return buf;
    }

    protected final ByteBuffer beginItem() {
        ensureInitialized();
        if (batchedItems == 0) {
            dataBuffer.clear();
            dataBuffer.position(HEADER_SIZE);
        }
        return dataBuffer;
    }

    protected final void endItem() {
        batchedItems++;
        if (DrawBatcher.isEnabled()) {
            DrawBatcher.register(this);
            if (batchedItems >= maxItems) {
                if (chunks.size() >= uniformRing - 1) {
                    DrawBatcher.drawImmediate(this);
                } else {
                    sealChunk();
                }
            }
        } else {
            DrawBatcher.drawImmediate(this);
        }
    }

    private void sealChunk() {
        if (batchedItems == 0) return;

        MinecraftClient client = MinecraftClient.getInstance();
        float fixedScreenWidth = client.getWindow().getFramebufferWidth() / FIXED_GUI_SCALE;
        float fixedScreenHeight = client.getWindow().getFramebufferHeight() / FIXED_GUI_SCALE;

        int endPosition = dataBuffer.position();
        dataBuffer.position(0);
        dataBuffer.putFloat(fixedScreenWidth);
        dataBuffer.putFloat(fixedScreenHeight);
        dataBuffer.putFloat(FIXED_GUI_SCALE);
        dataBuffer.putFloat(0f);
        dataBuffer.position(0);
        dataBuffer.limit(endPosition);

        GpuBuffer uniformBuffer = nextUniformBuffer();
        RenderSystem.getDevice().createCommandEncoder()
                .writeToBuffer(uniformBuffer.slice(0, dataBuffer.remaining()), dataBuffer);
        dataBuffer.limit(dataBuffer.capacity());

        chunks.add(new Chunk(uniformBuffer, batchedItems));
        batchedItems = 0;
    }

    @Override
    public final void uploadBatch(CommandEncoder encoder) {
        sealChunk();
    }

    @Override
    public final void drawBatch(RenderPass pass) {
        if (chunks.isEmpty()) return;

        pass.setPipeline(renderPipeline);
        pass.setVertexBuffer(0, dummyVertexBuffer);

        for (Chunk chunk : chunks) {
            pass.setUniform(uniformName, chunk.buffer());
            pass.draw(0, chunk.count() * 6);
        }

        chunks.clear();
    }

    @Override
    public final void discardBatch() {
        chunks.clear();
    }

    public final void flush() {
        if (batchedItems == 0 && chunks.isEmpty()) return;
        DrawBatcher.drawImmediate(this);
    }

    public void close() {
        batchedItems = 0;
        discardBatch();
        if (uniformBuffers != null) {
            for (GpuBuffer buf : uniformBuffers) {
                if (buf != null) buf.close();
            }
            uniformBuffers = null;
        }
        uniformRingIndex = 0;
        if (dummyVertexBuffer != null) {
            dummyVertexBuffer.close();
            dummyVertexBuffer = null;
        }
        if (dataBuffer != null) {
            MemoryUtil.memFree(dataBuffer);
            dataBuffer = null;
        }
        initialized = false;
    }
}
