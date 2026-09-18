package ru.white.proxymod.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import ru.white.proxymod.proxy.ProxyConfig;
import ru.white.proxymod.proxy.ProxyManager;
import ru.white.proxymod.render.Draw;
import ru.white.proxymod.render.Render2D;
import ru.white.proxymod.render.color.ColorUtil;
import ru.white.proxymod.render.font.Fonts;
import ru.white.proxymod.render.sound.GuiSounds;

public class ProxyButtonWidget extends ClickableWidget {

    private final Screen parent;

    public ProxyButtonWidget(int x, int y, int width, int height, Screen parent) {
        super(x, y, width, height, Text.literal("Proxy"));
        this.parent = parent;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (this.visible && (isHovered() || isMouseOver(click.x(), click.y()))) {
            if (click.button() == 0) {
                GuiSounds.button();
                MinecraftClient.getInstance().setScreen(new ProxyScreen(parent));
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        GuiSounds.button();
        MinecraftClient.getInstance().setScreen(new ProxyScreen(parent));
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        float currentScale = (float) MinecraftClient.getInstance().getWindow().getScaleFactor();
        float scaleFix = 2.0F / currentScale;

        float x = getX() / scaleFix;
        float y = getY() / scaleFix;
        float w = getWidth() / scaleFix;
        float h = getHeight() / scaleFix;

        boolean hover = isHovered();
        boolean active = ProxyManager.isProxyActive();
        ProxyConfig config = ProxyManager.getConfig();

        Render2D.beginOverlay();

        // Фон кнопки
        int bgCol = hover ? ColorUtil.getColor(18, 24, 38, 0.96F) : ColorUtil.getColor(12, 16, 26, 0.92F);
        Draw.rect(x, y, w, h, bgCol, 4.0F);

        // Рамка кнопки
        int borderCol;
        if (active) {
            borderCol = ColorUtil.getColor(78, 125, 255);
        } else if (hover) {
            borderCol = ColorUtil.getColor(70, 95, 135);
        } else {
            borderCol = ColorUtil.getColor(40, 52, 76);
        }
        Draw.outline(x, y, w, h, active ? 1.5F : 1.0F, borderCol, 4.0F);

        // Индикаторная точка слева
        float dotSize = 6.0F;
        float dotX = x + 9.0F;
        float dotY = y + h / 2.0F - dotSize / 2.0F;
        int dotCol = active ? ColorUtil.getColor(50, 225, 110) : ColorUtil.getColor(120, 130, 145);

        Draw.rect(dotX, dotY, dotSize, dotSize, dotCol, dotSize / 2.0F);

        // Текст кнопки
        String label = active ? "PROXY: " + config.getType().name() : "PROXY: OFF";
        float textX = dotX + dotSize + 7.0F;
        int textCol = active ? ColorUtil.getColor(245, 248, 255) : (hover ? ColorUtil.getColor(220, 230, 245) : ColorUtil.getColor(160, 170, 190));

        Fonts.sf_bold.draw(label, textX, y + h / 2.0F - 4.0F, 7.5F, textCol);

        Render2D.endOverlay();
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }
}
