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

public class RectPipeline extends UniformArrayPipeline {

    private static final Identifier PIPELINE_ID = Identifier.of("proxymod", "pipeline/rect");
    private static final Identifier VERTEX_SHADER = Identifier.of("proxymod", "core/rect");
    private static final Identifier FRAGMENT_SHADER = Identifier.of("proxymod", "core/rect");

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation(PIPELINE_ID)
                    .withVertexShader(VERTEX_SHADER)
                    .withFragmentShader(FRAGMENT_SHADER)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                    .withUniform("RectData", UniformType.UNIFORM_BUFFER)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private static final int MAX_RECTS = 64;
    private static final int RECT_SIZE = 14 * 16;
    private static final int UNIFORM_RING = 32;

    private final int[] colors9 = new int[9];

    public RectPipeline() {
        super("rect", PIPELINE, "RectData", MAX_RECTS, RECT_SIZE, UNIFORM_RING);
    }

    @Override
    public int batchLayer() {
        return 0;
    }

    public void drawRect(float x, float y, float width, float height,
                         int[] colors, float[] radii) {
        drawRect(x, y, width, height, colors, radii, 0f);
    }

    public void drawRect(float x, float y, float width, float height,
                         int[] colors, float[] radii, float innerBlur) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() == null) return;

        convertTo9Colors(colors);

        appendRect(x, y, width, height,
                x, y, width, height,
                innerBlur, radii, 0f, 0f, 1f, 0);
        endItem();
    }

    private void convertTo9Colors(int[] colors) {
        if (colors.length == 1) {
            for (int i = 0; i < 9; i++) {
                colors9[i] = colors[0];
            }
        } else if (colors.length == 4) {
            colors9[0] = colors[0];
            colors9[1] = blendColors(colors[0], colors[1]);
            colors9[2] = colors[1];
            colors9[3] = blendColors(colors[0], colors[3]);
            colors9[4] = blendColors(colors[0], colors[1], colors[2], colors[3]);
            colors9[5] = blendColors(colors[1], colors[2]);
            colors9[6] = colors[3];
            colors9[7] = blendColors(colors[3], colors[2]);
            colors9[8] = colors[2];
        } else if (colors.length >= 9) {
            System.arraycopy(colors, 0, colors9, 0, 9);
        } else {
            for (int i = 0; i < 9; i++) {
                colors9[i] = colors[i % colors.length];
            }
        }
    }

    private int blendColors(int... colors) {
        int r = 0, g = 0, b = 0, a = 0;
        for (int color : colors) {
            a += (color >> 24) & 0xFF;
            r += (color >> 16) & 0xFF;
            g += (color >> 8) & 0xFF;
            b += color & 0xFF;
        }
        int count = colors.length;
        return ((a / count) << 24) | ((r / count) << 16) | ((g / count) << 8) | (b / count);
    }

    private void appendRect(float drawX, float drawY, float drawWidth, float drawHeight,
                            float x, float y, float width, float height,
                            float innerBlur, float[] radii,
                            float glowSize, float glowStrength, float glowSoftness, int glowColor) {
        ByteBuffer dataBuffer = beginItem();

        dataBuffer.putFloat(drawX);
        dataBuffer.putFloat(drawY);
        dataBuffer.putFloat(drawWidth);
        dataBuffer.putFloat(drawHeight);

        dataBuffer.putFloat(radii[0]);
        dataBuffer.putFloat(radii[1]);
        dataBuffer.putFloat(radii[2]);
        dataBuffer.putFloat(radii[3]);

        for (int i = 0; i < 9; i++) {
            int color = colors9[i];
            float a = ((color >> 24) & 0xFF) / 255.0f;
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float bl = (color & 0xFF) / 255.0f;

            dataBuffer.putFloat(r);
            dataBuffer.putFloat(g);
            dataBuffer.putFloat(bl);
            dataBuffer.putFloat(a);
        }

        dataBuffer.putFloat(x);
        dataBuffer.putFloat(y);
        dataBuffer.putFloat(width);
        dataBuffer.putFloat(height);

        dataBuffer.putFloat(glowSize);
        dataBuffer.putFloat(glowStrength);
        dataBuffer.putFloat(Math.max(0.05f, glowSoftness));
        dataBuffer.putFloat(innerBlur);

        dataBuffer.putFloat(((glowColor >> 16) & 0xFF) / 255.0f);
        dataBuffer.putFloat(((glowColor >> 8) & 0xFF) / 255.0f);
        dataBuffer.putFloat((glowColor & 0xFF) / 255.0f);
        dataBuffer.putFloat(((glowColor >> 24) & 0xFF) / 255.0f);
    }
}
