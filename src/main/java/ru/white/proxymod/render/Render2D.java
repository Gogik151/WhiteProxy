package ru.white.proxymod.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import ru.white.proxymod.render.font.FontRenderer;
import ru.white.proxymod.render.font.Fonts;

public class Render2D {

    private static Render2D instance;

    public static Render2D getInstance() {
        if (instance == null) {
            instance = new Render2D();
        }
        return instance;
    }

    private final FontRenderer fontRenderer;
    private final RectPipeline rectPipeline;
    private final OutlinePipeline outlinePipeline;
    private boolean fontsLoaded = false;

    public Render2D() {
        this.fontRenderer = new FontRenderer();
        this.rectPipeline = new RectPipeline();
        this.outlinePipeline = new OutlinePipeline();
    }

    private void ensureFontsLoaded() {
        if (fontsLoaded) return;
        fontsLoaded = true;
        fontRenderer.loadAllFonts(Fonts.getRegistry());
    }

    public FontRenderer getFontRenderer() {
        ensureFontsLoaded();
        return fontRenderer;
    }

    public RectPipeline getRectPipeline() {
        return rectPipeline;
    }

    public OutlinePipeline getOutlinePipeline() {
        return outlinePipeline;
    }

    public void flushAll() {
        DrawBatcher.flushPending();
        rectPipeline.flush();
        outlinePipeline.flush();
        fontRenderer.flush();
    }

    public void close() {
        rectPipeline.close();
        outlinePipeline.close();
        fontRenderer.close();
    }

    private static boolean savedDepthTest = false;
    private static boolean savedDepthMask = false;
    private static boolean savedBlend = false;

    public static void beginOverlay() {
        DrawBatcher.setEnabled(true);

        savedDepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        savedBlend = GL11.glIsEnabled(GL11.GL_BLEND);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void endOverlay() {
        DrawBatcher.setEnabled(false);

        if (savedDepthMask) {
            GL11.glDepthMask(true);
        }
        if (savedDepthTest) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        } else {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
        if (!savedBlend) {
            GL11.glDisable(GL11.GL_BLEND);
        }
    }
}
