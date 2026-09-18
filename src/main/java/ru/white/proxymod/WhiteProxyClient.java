package ru.white.proxymod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import ru.white.proxymod.gui.ProxyScreen;
import ru.white.proxymod.proxy.ProxyConfig;
import ru.white.proxymod.proxy.ProxyManager;
import ru.white.proxymod.render.Draw;
import ru.white.proxymod.render.Render2D;
import ru.white.proxymod.render.color.ColorUtil;
import ru.white.proxymod.render.font.Fonts;

public class WhiteProxyClient implements ClientModInitializer {

    private static KeyBinding openKeyBinding;

    @Override
    public void onInitializeClient() {
        openKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.whiteproxy.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKeyBinding.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new ProxyScreen(null));
                }
            }
        });

        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options.hudHidden || mc.isInSingleplayer()) return;

            ProxyConfig cfg = ProxyManager.getConfig();
            if (!cfg.isEnabled() || !cfg.isHudEnabled()) return;

            ProxyConfig.ProxyProfile active = cfg.getActiveProfile();
            if (active == null) return;

            String typeStr = active.getType().getDisplayName();
            String hostStr = active.getHost() + ":" + active.getPort();
            long ping = active.getLastPingMs();
            String pingStr = ping > 0 ? (ping + "ms") : "OK";
            int pingColor = ping > 0 ? (ping < 120 ? ColorUtil.getColor(0, 255, 127) : (ping < 280 ? ColorUtil.getColor(255, 215, 0) : ColorUtil.getColor(255, 69, 0))) : ColorUtil.getColor(0, 229, 255);

            String text = "WhiteProxy: " + hostStr + " (" + typeStr + ")  " + pingStr;

            int padX = 8;
            float textW = Fonts.sf_medium.getWidth(text, 7f);
            float hudW = textW + padX * 2 + 10;
            float hudH = 18;

            int winW = mc.getWindow().getScaledWidth();
            float hudX = winW - hudW - 8;
            float hudY = 8;

            Render2D.beginOverlay();

            Draw.rect(hudX, hudY, hudW, hudH, ColorUtil.getColor(14, 18, 28, 200), 6f);
            Draw.outline(hudX, hudY, hudW, hudH, 1f, ColorUtil.getColor(255, 255, 255, 35), 6f);

            // Dot status
            Draw.rect(hudX + 8, hudY + 6.5f, 5, 5, pingColor, 2.5f);

            Fonts.sf_medium.draw(text, hudX + 18, hudY + 5.5f, 7f, ColorUtil.getColor(255));

            Render2D.endOverlay();
        });
    }
}
