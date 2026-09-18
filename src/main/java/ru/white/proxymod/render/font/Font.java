package ru.white.proxymod.render.font;

import ru.white.proxymod.render.Render2D;

public class Font {

    private final String name;

    public Font(String name) {
        this.name = name;
    }

    public void draw(String text, float x, float y, float size, int color) {
        Render2D.getInstance().getFontRenderer().drawText(name, text, x, y, size, color);
    }

    public void drawCentered(String text, float x, float y, float size, int color) {
        Render2D.getInstance().getFontRenderer().drawCenteredText(name, text, x, y, size, color);
    }

    public float getWidth(String text, float size) {
        return Render2D.getInstance().getFontRenderer().getTextWidth(name, text, size);
    }

    public float getHeight(float size) {
        return Render2D.getInstance().getFontRenderer().getTextHeight(name, size);
    }
}
