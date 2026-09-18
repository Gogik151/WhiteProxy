package ru.white.proxymod.render.color;

public class ColorUtil {

    public static int getColor(int red, int green, int blue, int alpha) {
        return ((alpha & 0xFF) << 24) |
                ((red & 0xFF) << 16) |
                ((green & 0xFF) << 8) |
                (blue & 0xFF);
    }

    public static int getColor(int red, int green, int blue, float alpha) {
        return getColor(red, green, blue, Math.round(alpha * 255.0F));
    }

    public static int getColor(int red, int green, int blue) {
        return getColor(red, green, blue, 255);
    }

    public static int getColor(int brightness, float alpha) {
        return getColor(brightness, brightness, brightness, Math.round(alpha * 255.0F));
    }

    public static int getColor(int brightness) {
        return getColor(brightness, brightness, brightness, 255);
    }

    public static int replAlpha(int color, float alpha) {
        int a = Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F);
        return (color & 0x00FFFFFF) | (a << 24);
    }

    public static int[] solid(int color) {
        return new int[]{color, color, color, color};
    }

    public static int[] solid8(int color) {
        return new int[]{color, color, color, color, color, color, color, color};
    }
}
