package ru.white.proxymod.render;

import ru.white.proxymod.render.color.ColorUtil;

public final class Draw {

    private Draw() {
    }

    private static Render2D r2d() {
        return Render2D.getInstance();
    }

    public static void flush() {
        r2d().flushAll();
    }

    public static void rect(float x, float y, float width, float height, int color) {
        rect(x, y, width, height, color, 0f);
    }

    public static void rect(float x, float y, float width, float height, int color, float radius) {
        rect(x, y, width, height, color, radius, radius, radius, radius);
    }

    public static void rect(float x, float y, float width, float height, int color,
                            float topLeft, float topRight, float bottomRight, float bottomLeft) {
        int[] colors = ColorUtil.solid(color);
        float[] radii = {topLeft, topRight, bottomRight, bottomLeft};
        r2d().getRectPipeline().drawRect(x, y, width, height, colors, radii);
    }

    public static void gradientRect(float x, float y, float width, float height,
                                    int[] colors, float radius) {
        gradientRect(x, y, width, height, colors, radius, radius, radius, radius);
    }

    public static void gradientRect(float x, float y, float width, float height,
                                    int[] colors, float topLeft, float topRight,
                                    float bottomRight, float bottomLeft) {
        float[] radii = {topLeft, topRight, bottomRight, bottomLeft};
        r2d().getRectPipeline().drawRect(x, y, width, height, colors, radii);
    }

    public static void outline(float x, float y, float width, float height, float thickness, int color) {
        outline(x, y, width, height, thickness, color, 0f, 0f, 0f, 0f);
    }

    public static void outline(float x, float y, float width, float height,
                               float thickness, int color, float radius) {
        outline(x, y, width, height, thickness, color, radius, radius, radius, radius);
    }

    public static void outline(float x, float y, float width, float height, float thickness, int color,
                               float topLeft, float topRight, float bottomRight, float bottomLeft) {
        int[] colors = ColorUtil.solid8(color);
        float[] thicknesses = {thickness, thickness, thickness, thickness,
                thickness, thickness, thickness, thickness};
        float[] radii = {topLeft, topRight, bottomRight, bottomLeft};
        r2d().getOutlinePipeline().drawOutline(x, y, width, height, colors, thicknesses, radii, 1.0f);
    }
}
