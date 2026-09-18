package ru.white.proxymod.render.font;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;
import ru.white.proxymod.render.DrawBatcher;
import ru.white.proxymod.render.color.ColorFormatting;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class FontPipeline implements DrawBatcher.Batched {

    private static final Identifier PIPELINE_ID = Identifier.of("proxymod", "pipeline/msdf");
    private static final Identifier SHADER_ID = Identifier.of("proxymod", "core/msdf");

    private static final float FIXED_GUI_SCALE = 2.0f;

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation(PIPELINE_ID)
                    .withVertexShader(SHADER_ID)
                    .withFragmentShader(SHADER_ID)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                    .withUniform("FontData", UniformType.UNIFORM_BUFFER)
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build());

    private static final int[] LEGACY_COLORS = new int[32];

    static {
        for (int i = 0; i < 16; ++i) {
            int j = (i >> 3 & 1) * 85;
            int r = (i >> 2 & 1) * 170 + j;
            int g = (i >> 1 & 1) * 170 + j;
            int b = (i & 1) * 170 + j;
            if (i == 6) r += 85;
            LEGACY_COLORS[i] = (255 << 24) | (r << 16) | (g << 8) | b;
            LEGACY_COLORS[i + 16] = ((r & 0xFCFCFC) >> 2 << 24) | (r << 16) | (g << 8) | b;
        }
    }

    private static final String LEGACY_CODE_CHARS = "0123456789abcdefklmnor";
    private static final int MAX_CHARS = 256;
    private static final int BUFFER_SIZE = 64 + MAX_CHARS * 64;
    private static final int UNIFORM_RING = 32;
    private static final int HEADER_SIZE = 64;

    private GpuBuffer[] uniformBuffers;
    private int uniformRingIndex = 0;
    private GpuBuffer dummyVertexBuffer;
    private ByteBuffer dataBuffer;
    private boolean initialized = false;

    private final Map<GpuTexture, GpuTextureView> textureViewCache = new HashMap<>();

    private int batchedChars = 0;
    private FontAtlas currentAtlas = null;
    private float currentOutlineWidth = 0;
    private int currentOutlineColor = 0;

    private record FontChunk(GpuBuffer buffer, GpuTextureView view, int count) {}
    private final ArrayList<FontChunk> chunks = new ArrayList<>();

    @Override
    public int batchLayer() {
        return 3;
    }

    private void appendGlyph(float x, float y, float w, float h,
                             float u0, float v0, float u1, float v1,
                             int color, float rotation, float pivotX, float pivotY, float glyphScale) {
        if (batchedChars == 0) {
            dataBuffer.clear();
            dataBuffer.position(HEADER_SIZE);
        }

        dataBuffer.putFloat(x);
        dataBuffer.putFloat(y);
        dataBuffer.putFloat(w);
        dataBuffer.putFloat(h);

        dataBuffer.putFloat(u0);
        dataBuffer.putFloat(v0);
        dataBuffer.putFloat(u1);
        dataBuffer.putFloat(v1);

        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        dataBuffer.putFloat(r);
        dataBuffer.putFloat(g);
        dataBuffer.putFloat(b);
        dataBuffer.putFloat(a);

        dataBuffer.putFloat(rotation);
        dataBuffer.putFloat(pivotX);
        dataBuffer.putFloat(pivotY);
        dataBuffer.putFloat(glyphScale);

        batchedChars++;
    }

    private int getFixedScaledWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return 960;
        return (int) Math.ceil((double) client.getWindow().getFramebufferWidth() / FIXED_GUI_SCALE);
    }

    private int getFixedScaledHeight() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return 540;
        return (int) Math.ceil((double) client.getWindow().getFramebufferHeight() / FIXED_GUI_SCALE);
    }

    private void ensureInitialized() {
        if (initialized) return;

        this.dataBuffer = MemoryUtil.memAlloc(BUFFER_SIZE);

        ByteBuffer dummyData = MemoryUtil.memAlloc(4);
        dummyData.putInt(0);
        dummyData.flip();
        this.dummyVertexBuffer = RenderSystem.getDevice().createBuffer(
                () -> "proxymod:font_dummy_vertex",
                GpuBuffer.USAGE_VERTEX,
                dummyData);
        MemoryUtil.memFree(dummyData);

        this.uniformBuffers = new GpuBuffer[UNIFORM_RING];
        initialized = true;
    }

    private GpuBuffer nextUniformBuffer() {
        GpuBuffer buf = uniformBuffers[uniformRingIndex];
        if (buf == null) {
            final int idx = uniformRingIndex;
            buf = RenderSystem.getDevice().createBuffer(
                    () -> "proxymod:font_uniform_" + idx,
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    BUFFER_SIZE);
            uniformBuffers[uniformRingIndex] = buf;
        }
        uniformRingIndex = (uniformRingIndex + 1) % UNIFORM_RING;
        return buf;
    }

    public void drawText(FontAtlas atlas, String text, float x, float y, float size, int color) {
        drawText(atlas, text, x, y, size, color, 0, 0, 0);
    }

    public void drawText(FontAtlas atlas, String text, float x, float y, float size, int color,
                         float outlineWidth, int outlineColor, float rotation) {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() == null) return;
        if (text == null || text.isEmpty()) return;

        text = ColorFormatting.get(text);

        atlas.ensureLoaded();
        if (atlas.getGlyphCount() == 0) return;

        ensureInitialized();

        if (DrawBatcher.isEnabled()) {
            DrawBatcher.register(this);
        }

        if (currentAtlas != null && (currentAtlas != atlas || currentOutlineWidth != outlineWidth
                || currentOutlineColor != outlineColor)) {
            flush();
        }

        currentAtlas = atlas;
        currentOutlineWidth = outlineWidth;
        currentOutlineColor = outlineColor;

        float scale = size / atlas.getFontSize();
        float cursorX = x;
        float cursorY = y;
        float rotationRad = (float) Math.toRadians(rotation);

        float pivotX = 0, pivotY = 0;
        if (rotation != 0) {
            pivotX = x + getTextWidth(atlas, text, size) / 2;
            pivotY = y + getTextHeight(atlas, text, size) / 2;
        }

        int currentColor = color;
        int i = 0;
        while (i < text.length()) {
            ColorAdvance colorAdvance = tryParseColorCode(text, i, color, currentColor);
            if (colorAdvance.matched()) {
                currentColor = colorAdvance.color();
                i += colorAdvance.skip();
                continue;
            }

            int codePoint = text.codePointAt(i);
            int charCount = Character.charCount(codePoint);

            if (codePoint == '\n') {
                cursorX = x;
                cursorY += atlas.getLineHeight() * scale;
                i += charCount;
                continue;
            }

            Glyph glyph = atlas.getGlyph(codePoint);
            if (glyph == null) {
                Glyph fallback = atlas.getGlyph('?');
                if (fallback != null) {
                    cursorX += fallback.xAdvance * scale;
                } else {
                    cursorX += size * 0.5f;
                }
                i += charCount;
                continue;
            }

            float glyphX = cursorX + glyph.xOffset * scale;
            float glyphY = cursorY + glyph.yOffset * scale;
            float glyphW = glyph.width * scale;
            float glyphH = glyph.height * scale;

            if (glyph.width > 0 && glyph.height > 0) {
                appendGlyph(
                        glyphX, glyphY, glyphW, glyphH,
                        glyph.u0, glyph.v0, glyph.u1, glyph.v1,
                        currentColor, rotationRad, pivotX, pivotY, scale);
            }

            cursorX += glyph.xAdvance * scale;

            if (batchedChars >= MAX_CHARS) {
                flush();
                currentAtlas = atlas;
            }

            i += charCount;
        }

        if (!DrawBatcher.isEnabled()) {
            flush();
        }
    }

    private void sealChunk() {
        if (batchedChars == 0 || currentAtlas == null) {
            batchedChars = 0;
            currentAtlas = null;
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() == null) {
            batchedChars = 0;
            currentAtlas = null;
            return;
        }

        AbstractTexture texture = client.getTextureManager().getTexture(currentAtlas.getTextureId());
        if (texture == null) {
            batchedChars = 0;
            currentAtlas = null;
            return;
        }

        GpuTexture gpuTexture;
        try {
            gpuTexture = texture.getGlTexture();
        } catch (Exception e) {
            batchedChars = 0;
            currentAtlas = null;
            return;
        }

        if (chunks.size() >= UNIFORM_RING - 1) {
            DrawBatcher.drawImmediate(this, false);
        }

        int count = batchedChars;
        int endPosition = dataBuffer.position();

        dataBuffer.position(0);
        dataBuffer.putFloat(getFixedScaledWidth());
        dataBuffer.putFloat(getFixedScaledHeight());
        dataBuffer.putFloat(FIXED_GUI_SCALE);
        dataBuffer.putFloat(currentOutlineWidth);

        dataBuffer.putFloat(((currentOutlineColor >> 16) & 0xFF) / 255.0f);
        dataBuffer.putFloat(((currentOutlineColor >> 8) & 0xFF) / 255.0f);
        dataBuffer.putFloat((currentOutlineColor & 0xFF) / 255.0f);
        dataBuffer.putFloat(((currentOutlineColor >> 24) & 0xFF) / 255.0f);

        dataBuffer.putFloat(currentAtlas.getAtlasWidth());
        dataBuffer.putFloat(currentAtlas.getAtlasHeight());
        dataBuffer.putFloat(currentAtlas.getDistanceRange());
        dataBuffer.putFloat(currentAtlas.getFontSize());

        dataBuffer.putInt(count);
        dataBuffer.putInt(0);
        dataBuffer.putInt(0);
        dataBuffer.putInt(0);

        dataBuffer.position(0);
        dataBuffer.limit(endPosition);

        GpuBuffer uniformBuffer = nextUniformBuffer();
        RenderSystem.getDevice().createCommandEncoder()
                .writeToBuffer(uniformBuffer.slice(0, dataBuffer.remaining()), dataBuffer);
        dataBuffer.limit(dataBuffer.capacity());

        chunks.add(new FontChunk(uniformBuffer, getCachedTextureView(gpuTexture), count));

        batchedChars = 0;
        currentAtlas = null;
    }

    @Override
    public void uploadBatch(CommandEncoder encoder) {
        sealChunk();
    }

    @Override
    public void drawBatch(RenderPass pass) {
        if (chunks.isEmpty()) return;

        GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

        pass.setPipeline(PIPELINE);
        pass.setVertexBuffer(0, dummyVertexBuffer);

        for (FontChunk chunk : chunks) {
            if (chunk.view().isClosed()) continue;
            pass.bindTexture("Sampler0", chunk.view(), sampler);
            pass.setUniform("FontData", chunk.buffer());
            pass.draw(0, chunk.count() * 6);
        }

        chunks.clear();
    }

    @Override
    public void discardBatch() {
        chunks.clear();
    }

    public void flush() {
        sealChunk();
        if (!DrawBatcher.isEnabled() && !chunks.isEmpty()) {
            DrawBatcher.drawImmediate(this, false);
        }
    }

    private GpuTextureView getCachedTextureView(GpuTexture gpuTexture) {
        GpuTextureView view = textureViewCache.get(gpuTexture);
        if (view == null || view.isClosed()) {
            textureViewCache.entrySet().removeIf(e -> {
                if (e.getKey().isClosed() || e.getValue().isClosed()) {
                    if (!e.getValue().isClosed()) e.getValue().close();
                    return true;
                }
                return false;
            });
            view = RenderSystem.getDevice().createTextureView(gpuTexture);
            textureViewCache.put(gpuTexture, view);
        }
        return view;
    }

    public float getTextWidth(FontAtlas atlas, String text, float size) {
        atlas.ensureLoaded();
        float scale = size / atlas.getFontSize();

        Float cached = atlas.widthCache.get(text);
        if (cached != null) {
            return cached * scale;
        }

        String stripped = ColorFormatting.stripForWidth(text);
        Float base = stripped.equals(text) ? null : atlas.widthCache.get(stripped);
        if (base == null) {
            base = computeBaseWidth(atlas, stripped);
            atlas.widthCache.put(stripped, base);
        }
        atlas.widthCache.put(text, base);
        return base * scale;
    }

    private float computeBaseWidth(FontAtlas atlas, String text) {
        float width = 0;
        float maxWidth = 0;

        int i = 0;
        while (i < text.length()) {
            ColorAdvance colorAdvance = trySkipColorCode(text, i);
            if (colorAdvance.matched()) {
                i += colorAdvance.skip();
                continue;
            }

            int codePoint = text.codePointAt(i);
            int charCount = Character.charCount(codePoint);

            if (codePoint == '\n') {
                maxWidth = Math.max(maxWidth, width);
                width = 0;
                i += charCount;
                continue;
            }

            Glyph glyph = atlas.getGlyph(codePoint);
            if (glyph != null) {
                width += glyph.xAdvance;
            } else {
                Glyph fallback = atlas.getGlyph('?');
                if (fallback != null) {
                    width += fallback.xAdvance;
                } else {
                    width += atlas.getFontSize() * 0.5f;
                }
            }

            i += charCount;
        }

        return Math.max(maxWidth, width);
    }

    public float getTextHeight(FontAtlas atlas, String text, float size) {
        atlas.ensureLoaded();
        float scale = size / atlas.getFontSize();
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') lines++;
        }
        return lines * atlas.getLineHeight() * scale;
    }

    private record ColorAdvance(int skip, int color, boolean matched) {}

    private ColorAdvance tryParseColorCode(String text, int index, int defaultColor, int currentColor) {
        ColorFormatting.ColorTag tag = ColorFormatting.parseTag(text, index, defaultColor);
        if (tag != null) {
            return new ColorAdvance(tag.length(), tag.color(), true);
        }

        int codePoint = text.codePointAt(index);
        int charCount = Character.charCount(codePoint);
        if ((codePoint == '\u00A7' || codePoint == '&') && index + charCount < text.length()) {
            int nextCodePoint = text.codePointAt(index + charCount);
            if (nextCodePoint == '#' && index + charCount + 6 < text.length()) {
                try {
                    String hex = text.substring(index + charCount + 1, index + charCount + 7);
                    int parsed = (0xFF << 24) | Integer.parseInt(hex, 16);
                    return new ColorAdvance(charCount + 7, parsed, true);
                } catch (Exception ignored) {
                }
            }
            int code = LEGACY_CODE_CHARS.indexOf(Character.toLowerCase((char) nextCodePoint));
            if (code >= 0) {
                int parsed = code < 16 ? LEGACY_COLORS[code] : (code == 21 ? defaultColor : currentColor);
                return new ColorAdvance(charCount + Character.charCount(nextCodePoint), parsed, true);
            }
        }

        return new ColorAdvance(0, currentColor, false);
    }

    private ColorAdvance trySkipColorCode(String text, int index) {
        return tryParseColorCode(text, index, 0, 0);
    }

    public void close() {
        batchedChars = 0;
        currentAtlas = null;
        discardBatch();
        for (GpuTextureView view : textureViewCache.values()) {
            if (!view.isClosed()) view.close();
        }
        textureViewCache.clear();
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
