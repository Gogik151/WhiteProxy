package ru.white.proxymod.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

import java.nio.ByteBuffer;

public class OutlinePipeline extends UniformArrayPipeline {

    private static final Identifier PIPELINE_ID = Identifier.of("proxymod", "pipeline/outline");
    private static final Identifier VERTEX_SHADER = Identifier.of("proxymod", "core/outline");
    private static final Identifier FRAGMENT_SHADER = Identifier.of("proxymod", "core/outline");

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation(PIPELINE_ID)
                    .withVertexShader(VERTEX_SHADER)
                    .withFragmentShader(FRAGMENT_SHADER)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                    .withUniform("OutlineData", UniformType.UNIFORM_BUFFER)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private static final int MAX_OUTLINES = 64;
    private static final int OUTLINE_SIZE = 13 * 16;
    private static final int UNIFORM_RING = 32;

    public OutlinePipeline() {
        super("outline", PIPELINE, "OutlineData", MAX_OUTLINES, OUTLINE_SIZE, UNIFORM_RING);
    }

    @Override
    public int batchLayer() {
        return 1;
    }

    public void drawOutline(float x, float y, float width, float height,
                            int[] colors, float[] thicknesses, float[] radii, float smoothness) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() == null) return;

        ByteBuffer dataBuffer = beginItem();

        dataBuffer.putFloat(x);
        dataBuffer.putFloat(y);
        dataBuffer.putFloat(width);
        dataBuffer.putFloat(height);

        dataBuffer.putFloat(radii[0]);
        dataBuffer.putFloat(radii[1]);
        dataBuffer.putFloat(radii[2]);
        dataBuffer.putFloat(radii[3]);

        for (int i = 0; i < 8; i++) {
            int color = i < colors.length ? colors[i] : colors[colors.length - 1];
            float a = ((color >> 24) & 0xFF) / 255.0f;
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;

            dataBuffer.putFloat(r);
            dataBuffer.putFloat(g);
            dataBuffer.putFloat(b);
            dataBuffer.putFloat(a);
        }

        for (int i = 0; i < 8; i++) {
            float t = i < thicknesses.length ? thicknesses[i] : thicknesses[thicknesses.length - 1];
            dataBuffer.putFloat(t);
        }

        dataBuffer.putFloat(smoothness);
        dataBuffer.putFloat(0f);
        dataBuffer.putFloat(0f);
        dataBuffer.putFloat(0f);

        endItem();
    }
}
