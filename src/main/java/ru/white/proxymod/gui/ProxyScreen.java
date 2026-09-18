package ru.white.proxymod.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import ru.white.proxymod.proxy.ProxyConfig;
import ru.white.proxymod.proxy.ProxyManager;
import ru.white.proxymod.render.Draw;
import ru.white.proxymod.render.Render2D;
import ru.white.proxymod.render.color.ColorUtil;
import ru.white.proxymod.render.font.Fonts;
import ru.white.proxymod.render.math.MathUtil;
import ru.white.proxymod.render.sound.GuiSounds;

public class ProxyScreen extends Screen {

    private final Screen parent;
    private final ProxyConfig config;

    private boolean enabled;
    private ProxyConfig.Type selectedType;
    private String currentLang;

    private enum Field { NONE, HOST, PORT, USER, PASS }
    private Field focusedField = Field.NONE;

    private String hostText;
    private String portText;
    private String userText;
    private String passText;

    private ProxyManager.TestResult lastTestResult = null;
    private boolean isTesting = false;

    private final float winW = 340.0F;
    private final float winH = 192.0F;

    public ProxyScreen(Screen parent) {
        super(Text.literal("Proxy Settings"));
        this.parent = parent;
        this.config = ProxyManager.getConfig();
        this.enabled = config.isEnabled();
        this.selectedType = config.getType();
        this.currentLang = config.getLanguage();

        this.hostText = config.getHost() != null ? config.getHost() : "127.0.0.1";
        this.portText = String.valueOf(config.getPort() > 0 ? config.getPort() : 1080);
        this.userText = config.getUsername() != null ? config.getUsername() : "";
        this.passText = config.getPassword() != null ? config.getPassword() : "";
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float currentScale = (float) mc.getWindow().getScaleFactor();
        float scaleFix = 2.0F / currentScale;

        float screenW = mc.getWindow().getScaledWidth() / scaleFix;
        float screenH = mc.getWindow().getScaledHeight() / scaleFix;

        float mx = mouseX / scaleFix;
        float my = mouseY / scaleFix;

        float winX = screenW / 2.0F - winW / 2.0F;
        float winY = screenH / 2.0F - winH / 2.0F;

        Render2D.beginOverlay();

        // 1. Атмосферный темный градиентный фон
        int cTop = ColorUtil.getColor(5, 7, 14, 0.72F);
        int cBottom = ColorUtil.getColor(10, 14, 25, 0.88F);
        Draw.gradientRect(0, 0, screenW, screenH, new int[]{cTop, cTop, cBottom, cBottom}, 0);

        // 2. Тонкая кибер-сетка
        float gridSize = 32.0F;
        int gridColor = ColorUtil.getColor(50, 90, 175, 0.035F);
        for (float gx = 0; gx < screenW; gx += gridSize) {
            Draw.rect(gx, 0, 1.0F, screenH, gridColor);
        }
        for (float gy = 0; gy < screenH; gy += gridSize) {
            Draw.rect(0, gy, screenW, 1.0F, gridColor);
        }

        // 3. Плавно движущиеся частицы данных
        long time = System.currentTimeMillis();
        for (int i = 0; i < 18; i++) {
            float seedX = (float) ((i * 137 + 43) % (int) Math.max(10, screenW));
            float seedY = (float) ((i * 219 + 71) % (int) Math.max(10, screenH));
            float speed = 0.012F + (i % 5) * 0.006F;
            float nx = (seedX + (time * speed)) % screenW;
            float ny = (seedY + (time * speed * 0.4F)) % screenH;
            float pulse = (float) (0.5 + 0.5 * Math.sin((time * 0.002) + i));
            int nodeCol = ColorUtil.getColor(60, 130, 255, 0.05F + 0.07F * pulse);
            Draw.rect(nx, ny, 2.0F, 2.0F, nodeCol, 1.0F);
        }

        int accent = ColorUtil.getColor(78, 125, 255);

        // 4. Неоновый ореол вокруг карточки окна
        Draw.rect(winX - 26.0F, winY - 26.0F, winW + 52.0F, winH + 52.0F, ColorUtil.getColor(18, 60, 170, 0.12F), 22.0F);
        Draw.rect(winX - 10.0F, winY - 10.0F, winW + 20.0F, winH + 20.0F, ColorUtil.getColor(35, 95, 230, 0.16F), 12.0F);

        // 5. Тело модального окна
        Draw.rect(winX, winY, winW, winH, ColorUtil.getColor(12, 16, 26, 0.98F), 6.0F);
        Draw.outline(winX, winY, winW, winH, 1.5F, ColorUtil.getColor(38, 50, 74), 6.0F);

        // Верхняя акцент-линия
        Draw.gradientRect(winX + 6.0F, winY, winW - 12.0F, 2.0F,
                new int[]{accent, ColorUtil.getColor(0, 215, 255), ColorUtil.getColor(0, 215, 255), accent}, 1.0F);

        // 6. Шапка окна (Заголовок, RU/EN переключатель, Кнопка закрытия)
        String title = I18n.title(currentLang);
        String subtitle = I18n.subtitle(currentLang);
        Fonts.sf_bold.draw(title, winX + 14.0F, winY + 9.0F, 8.5F, ColorUtil.getColor(245));
        Fonts.sf_medium.draw(subtitle, winX + 14.0F + Fonts.sf_bold.getWidth(title, 8.5F) + 6.0F, winY + 10.5F, 6.2F, accent);

        // Кнопка закрытия X (четкая и видимая)
        float closeSize = 16.0F;
        float closeX = winX + winW - 14.0F - closeSize;
        float closeY = winY + 6.0F;
        boolean closeHover = MathUtil.isHovered(mx, my, closeX, closeY, closeSize, closeSize);

        int closeBg = closeHover ? ColorUtil.getColor(240, 50, 50, 0.25F) : ColorUtil.getColor(20, 26, 38, 0.65F);
        int closeBorder = closeHover ? ColorUtil.getColor(255, 75, 75, 0.85F) : ColorUtil.getColor(36, 48, 70);
        Draw.rect(closeX, closeY, closeSize, closeSize, closeBg, 3.5F);
        Draw.outline(closeX, closeY, closeSize, closeSize, 1.0F, closeBorder, 3.5F);
        Fonts.sf_bold.drawCentered("X", closeX + closeSize / 2.0F, closeY + 3.5F, 7.2F,
                closeHover ? ColorUtil.getColor(255, 110, 110) : ColorUtil.getColor(160, 170, 190));

        // Переключатель языков [ RU | EN ]
        float langW = 46.0F;
        float langH = 16.0F;
        float langX = closeX - 6.0F - langW;
        float langY = closeY;

        boolean isRu = I18n.isRu(currentLang);
        boolean ruHover = MathUtil.isHovered(mx, my, langX, langY, 23.0F, langH);
        boolean enHover = MathUtil.isHovered(mx, my, langX + 23.0F, langY, 23.0F, langH);

        Draw.rect(langX, langY, langW, langH, ColorUtil.getColor(15, 20, 32, 0.85F), 3.5F);
        Draw.outline(langX, langY, langW, langH, 1.0F, ColorUtil.getColor(36, 48, 70), 3.5F);

        if (isRu) {
            Draw.rect(langX + 1.0F, langY + 1.0F, 21.5F, langH - 2.0F, accent, 2.5F);
            Fonts.sf_bold.drawCentered("RU", langX + 11.5F, langY + 3.5F, 6.5F, ColorUtil.getColor(255));
        } else {
            int ruCol = ruHover ? ColorUtil.getColor(225, 235, 250) : ColorUtil.getColor(125, 135, 155);
            Fonts.sf_medium.drawCentered("RU", langX + 11.5F, langY + 3.5F, 6.5F, ruCol);
        }

        Draw.rect(langX + 23.0F, langY + 3.0F, 1.0F, langH - 6.0F, ColorUtil.getColor(36, 48, 70));

        if (!isRu) {
            Draw.rect(langX + 23.5F, langY + 1.0F, 21.5F, langH - 2.0F, accent, 2.5F);
            Fonts.sf_bold.drawCentered("EN", langX + 34.5F, langY + 3.5F, 6.5F, ColorUtil.getColor(255));
        } else {
            int enCol = enHover ? ColorUtil.getColor(225, 235, 250) : ColorUtil.getColor(125, 135, 155);
            Fonts.sf_medium.drawCentered("EN", langX + 34.5F, langY + 3.5F, 6.5F, enCol);
        }

        // Разделитель шапки
        Draw.rect(winX, winY + 26.0F, winW, 1.0F, ColorUtil.getColor(28, 38, 56));

        // 7. Переключатель активности прокси
        float switchW = 25.0F;
        float switchH = 13.0F;
        float switchX = winX + winW - 14.0F - switchW;
        float switchY = winY + 31.0F;

        Fonts.sf_medium.draw(I18n.useProxy(currentLang), winX + 14.0F, winY + 33.5F, 7.2F, ColorUtil.getColor(235, 240, 250));

        int trackCol = enabled ? accent : ColorUtil.getColor(22, 30, 44);
        Draw.rect(switchX, switchY, switchW, switchH, trackCol, switchH / 2.0F);
        Draw.outline(switchX, switchY, switchW, switchH, 1.0F, enabled ? accent : ColorUtil.getColor(44, 58, 82), switchH / 2.0F);

        float thumbSize = switchH - 3.0F;
        float thumbX = switchX + 1.5F + (enabled ? (switchW - thumbSize - 3.0F) : 0.0F);
        Draw.rect(thumbX, switchY + 1.5F, thumbSize, thumbSize, ColorUtil.getColor(255), thumbSize / 2.0F);

        // 8. Выбор протокола
        Fonts.sf_regular.draw(I18n.protocol(currentLang), winX + 14.0F, winY + 49.0F, 5.8F, ColorUtil.getColor(130, 140, 160));

        ProxyConfig.Type[] types = ProxyConfig.Type.values();
        float tabGap = 6.0F;
        float tabW = (winW - 28.0F - (types.length - 1) * tabGap) / types.length;
        float tabH = 16.0F;
        float tabY = winY + 57.0F;

        for (int i = 0; i < types.length; i++) {
            ProxyConfig.Type t = types[i];
            float tabX = winX + 14.0F + i * (tabW + tabGap);
            boolean isSel = (selectedType == t);
            boolean isHov = MathUtil.isHovered(mx, my, tabX, tabY, tabW, tabH);

            int tabBg = isSel ? ColorUtil.replAlpha(accent, 0.28F) : (isHov ? ColorUtil.getColor(20, 28, 42) : ColorUtil.getColor(14, 18, 28));
            int tabBorder = isSel ? accent : (isHov ? ColorUtil.getColor(54, 72, 104) : ColorUtil.getColor(28, 38, 54));

            Draw.rect(tabX, tabY, tabW, tabH, tabBg, 3.0F);
            Draw.outline(tabX, tabY, tabW, tabH, 1.0F, tabBorder, 3.0F);
            Fonts.sf_bold.drawCentered(t.getDisplayName(), tabX + tabW / 2.0F, tabY + 4.0F, 6.8F,
                    isSel ? ColorUtil.getColor(255) : (isHov ? ColorUtil.getColor(215, 225, 240) : ColorUtil.getColor(145, 155, 175)));
        }

        // 9. Поля ввода Host и Port
        float hostH = 18.0F;
        float hostY = winY + 86.0F;
        float portW = 58.0F;
        float hostW = winW - 28.0F - portW - 8.0F;
        float hostX = winX + 14.0F;
        float portX = hostX + hostW + 8.0F;

        drawInputField(hostX, hostY, hostW, hostH, I18n.hostLabel(currentLang), hostText, "127.0.0.1", false, focusedField == Field.HOST, mx, my);
        drawInputField(portX, hostY, portW, hostH, I18n.portLabel(currentLang), portText, "1080", false, focusedField == Field.PORT, mx, my);

        // 10. Поля ввода Login и Password
        float credsY = winY + 117.0F;
        float credsW = (winW - 28.0F - 8.0F) / 2.0F;
        float userX = winX + 14.0F;
        float passX = userX + credsW + 8.0F;

        drawInputField(userX, credsY, credsW, hostH, I18n.userLabel(currentLang), userText, I18n.userPlaceholder(currentLang), false, focusedField == Field.USER, mx, my);
        drawInputField(passX, credsY, credsW, hostH, I18n.passLabel(currentLang), passText, I18n.passPlaceholder(currentLang), true, focusedField == Field.PASS, mx, my);

        // 11. Статус проверки подключения
        float statusY = winY + 141.0F;
        if (isTesting) {
            Fonts.sf_medium.draw(I18n.testing(currentLang), winX + 14.0F, statusY, 6.5F, ColorUtil.getColor(245, 195, 60));
        } else if (lastTestResult != null) {
            int statCol = lastTestResult.success() ? ColorUtil.getColor(50, 225, 110) : ColorUtil.getColor(245, 80, 80);
            Draw.rect(winX + 14.0F, statusY + 2.0F, 4.0F, 4.0F, statCol, 2.0F);

            String statusStr = lastTestResult.success()
                    ? I18n.success(currentLang, lastTestResult.pingMs())
                    : I18n.failed(currentLang, lastTestResult.error());
            Fonts.sf_medium.draw(statusStr, winX + 22.0F, statusY, 6.5F, statCol);
        }

        // 12. Кнопки в подвале (без пустого зазора)
        float btnH = 20.0F;
        float btnY = winY + 158.0F;

        // [ Проверить / Check ]
        float testBtnW = 76.0F;
        float testBtnX = winX + 14.0F;
        boolean testHov = MathUtil.isHovered(mx, my, testBtnX, btnY, testBtnW, btnH);
        Draw.rect(testBtnX, btnY, testBtnW, btnH, testHov ? ColorUtil.getColor(24, 34, 50) : ColorUtil.getColor(16, 22, 34), 3.5F);
        Draw.outline(testBtnX, btnY, testBtnW, btnH, 1.0F, testHov ? ColorUtil.getColor(62, 82, 116) : ColorUtil.getColor(32, 44, 66), 3.5F);
        Fonts.sf_medium.drawCentered(I18n.checkBtn(currentLang), testBtnX + testBtnW / 2.0F, btnY + 5.5F, 6.8F,
                testHov ? ColorUtil.getColor(240) : ColorUtil.getColor(185, 195, 215));

        // [ Отмена / Cancel ]
        float cancelBtnW = 65.0F;
        float cancelBtnX = winX + winW - 14.0F - cancelBtnW;
        boolean cancelHov = MathUtil.isHovered(mx, my, cancelBtnX, btnY, cancelBtnW, btnH);
        Draw.rect(cancelBtnX, btnY, cancelBtnW, btnH, cancelHov ? ColorUtil.getColor(26, 34, 48) : ColorUtil.getColor(16, 22, 32), 3.5F);
        Draw.outline(cancelBtnX, btnY, cancelBtnW, btnH, 1.0F, cancelHov ? ColorUtil.getColor(56, 70, 94) : ColorUtil.getColor(30, 40, 56), 3.5F);
        Fonts.sf_medium.drawCentered(I18n.cancelBtn(currentLang), cancelBtnX + cancelBtnW / 2.0F, btnY + 5.5F, 6.8F,
                cancelHov ? ColorUtil.getColor(215) : ColorUtil.getColor(155, 165, 180));

        // [ Сохранить / Save ]
        float saveBtnW = 85.0F;
        float saveBtnX = cancelBtnX - saveBtnW - 8.0F;
        boolean saveHov = MathUtil.isHovered(mx, my, saveBtnX, btnY, saveBtnW, btnH);
        Draw.rect(saveBtnX, btnY, saveBtnW, btnH, saveHov ? ColorUtil.replAlpha(accent, 0.45F) : ColorUtil.replAlpha(accent, 0.30F), 3.5F);
        Draw.outline(saveBtnX, btnY, saveBtnW, btnH, 1.0F, accent, 3.5F);
        Fonts.sf_bold.drawCentered(I18n.saveBtn(currentLang), saveBtnX + saveBtnW / 2.0F, btnY + 5.5F, 7.0F, ColorUtil.getColor(255));

        Render2D.endOverlay();
    }

    private void drawInputField(float x, float y, float w, float h, String label, String text, String placeholder, boolean isPassword, boolean focused, float mx, float my) {
        Fonts.sf_regular.draw(label, x, y - 8.5F, 5.8F, ColorUtil.getColor(130, 140, 162));

        boolean hov = MathUtil.isHovered(mx, my, x, y, w, h);
        int bg = focused ? ColorUtil.getColor(18, 24, 38) : (hov ? ColorUtil.getColor(15, 20, 32) : ColorUtil.getColor(10, 14, 22));
        Draw.rect(x, y, w, h, bg, 3.0F);

        int border = focused ? ColorUtil.getColor(78, 125, 255) : (hov ? ColorUtil.getColor(52, 68, 98) : ColorUtil.getColor(30, 40, 58));
        Draw.outline(x, y, w, h, focused ? 1.5F : 1.0F, border, 3.0F);

        boolean blink = focused && (System.currentTimeMillis() / 500L) % 2L == 0L;
        if (text.isEmpty()) {
            if (focused) {
                if (blink) {
                    Draw.rect(x + 6.0F, y + 3.5F, 1.2F, h - 7.0F, ColorUtil.getColor(78, 125, 255), 0.5F);
                }
            } else {
                Fonts.sf_regular.draw(placeholder, x + 6.0F, y + 4.5F, 7.0F, ColorUtil.getColor(85, 95, 115));
            }
        } else {
            String display = isPassword ? "•".repeat(text.length()) : text;
            Fonts.sf_regular.draw(display, x + 6.0F, y + 4.5F, 7.0F, ColorUtil.getColor(240, 245, 255));
            if (blink) {
                float textW = Fonts.sf_regular.getWidth(display, 7.0F);
                Draw.rect(x + 6.0F + textW + 1.0F, y + 3.5F, 1.2F, h - 7.0F, ColorUtil.getColor(78, 125, 255), 0.5F);
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float currentScale = (float) mc.getWindow().getScaleFactor();
        float scaleFix = 2.0F / currentScale;

        float screenW = mc.getWindow().getScaledWidth() / scaleFix;
        float screenH = mc.getWindow().getScaledHeight() / scaleFix;

        float mx = (float) (click.x() / scaleFix);
        float my = (float) (click.y() / scaleFix);
        int button = click.button();

        float winX = screenW / 2.0F - winW / 2.0F;
        float winY = screenH / 2.0F - winH / 2.0F;

        if (button == 0) {
            // Кнопка закрытия X
            float closeSize = 16.0F;
            float closeX = winX + winW - 14.0F - closeSize;
            float closeY = winY + 6.0F;
            if (MathUtil.isHovered(mx, my, closeX, closeY, closeSize, closeSize)) {
                GuiSounds.close();
                close();
                return true;
            }

            // Переключатель языка RU / EN
            float langW = 46.0F;
            float langH = 16.0F;
            float langX = closeX - 6.0F - langW;
            float langY = closeY;

            if (MathUtil.isHovered(mx, my, langX, langY, 23.0F, langH)) {
                if (!I18n.isRu(currentLang)) {
                    currentLang = "ru";
                    config.setLanguage("ru");
                    config.save();
                    GuiSounds.button();
                }
                return true;
            } else if (MathUtil.isHovered(mx, my, langX + 23.0F, langY, 23.0F, langH)) {
                if (I18n.isRu(currentLang)) {
                    currentLang = "en";
                    config.setLanguage("en");
                    config.save();
                    GuiSounds.button();
                }
                return true;
            }

            // Переключатель активности
            float switchW = 25.0F;
            float switchH = 13.0F;
            float switchY = winY + 31.0F;
            if (MathUtil.isHovered(mx, my, winX + 14.0F, switchY - 3.0F, winW - 28.0F, switchH + 6.0F)) {
                enabled = !enabled;
                GuiSounds.toggle(enabled);
                return true;
            }

            // Табы протокола
            ProxyConfig.Type[] types = ProxyConfig.Type.values();
            float tabGap = 6.0F;
            float tabW = (winW - 28.0F - (types.length - 1) * tabGap) / types.length;
            float tabH = 16.0F;
            float tabY = winY + 57.0F;

            for (int i = 0; i < types.length; i++) {
                float tabX = winX + 14.0F + i * (tabW + tabGap);
                if (MathUtil.isHovered(mx, my, tabX, tabY, tabW, tabH)) {
                    selectedType = types[i];
                    GuiSounds.picker(true);
                    return true;
                }
            }

            // Поля ввода
            float hostH = 18.0F;
            float hostY = winY + 86.0F;
            float portW = 58.0F;
            float hostW = winW - 28.0F - portW - 8.0F;
            float hostX = winX + 14.0F;
            float portX = hostX + hostW + 8.0F;

            float credsY = winY + 117.0F;
            float credsW = (winW - 28.0F - 8.0F) / 2.0F;
            float userX = winX + 14.0F;
            float passX = userX + credsW + 8.0F;

            if (MathUtil.isHovered(mx, my, hostX, hostY, hostW, hostH)) {
                focusedField = Field.HOST;
                return true;
            } else if (MathUtil.isHovered(mx, my, portX, hostY, portW, hostH)) {
                focusedField = Field.PORT;
                return true;
            } else if (MathUtil.isHovered(mx, my, userX, credsY, credsW, hostH)) {
                focusedField = Field.USER;
                return true;
            } else if (MathUtil.isHovered(mx, my, passX, credsY, credsW, hostH)) {
                focusedField = Field.PASS;
                return true;
            } else {
                focusedField = Field.NONE;
            }

            // Кнопки подвала
            float btnH = 20.0F;
            float btnY = winY + 158.0F;

            // [ Проверить / Check ]
            float testBtnW = 76.0F;
            float testBtnX = winX + 14.0F;
            if (MathUtil.isHovered(mx, my, testBtnX, btnY, testBtnW, btnH)) {
                runProxyTest();
                GuiSounds.button();
                return true;
            }

            // [ Отмена / Cancel ]
            float cancelBtnW = 65.0F;
            float cancelBtnX = winX + winW - 14.0F - cancelBtnW;
            if (MathUtil.isHovered(mx, my, cancelBtnX, btnY, cancelBtnW, btnH)) {
                GuiSounds.close();
                close();
                return true;
            }

            // [ Сохранить / Save ]
            float saveBtnW = 85.0F;
            float saveBtnX = cancelBtnX - saveBtnW - 8.0F;
            if (MathUtil.isHovered(mx, my, saveBtnX, btnY, saveBtnW, btnH)) {
                saveConfig();
                GuiSounds.button();
                close();
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (!input.isValidChar() || focusedField == Field.NONE) {
            return super.charTyped(input);
        }
        String str = input.asString();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (focusedField) {
                case HOST -> {
                    if (c == ':') {
                        focusedField = Field.PORT;
                        portText = "";
                    } else if (hostText.length() < 128 && (Character.isLetterOrDigit(c) || c == '.' || c == '-')) {
                        hostText += c;
                    }
                }
                case PORT -> {
                    if (portText.length() < 5 && Character.isDigit(c)) {
                        portText += c;
                    }
                }
                case USER -> {
                    if (userText.length() < 64 && c >= 32 && c != 127) {
                        userText += c;
                    }
                }
                case PASS -> {
                    if (passText.length() < 64 && c >= 32 && c != 127) {
                        passText += c;
                    }
                }
                default -> {}
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int key = keyInput.key();
        long win = MinecraftClient.getInstance().getWindow().getHandle();
        boolean ctrl = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            GuiSounds.close();
            close();
            return true;
        }

        if (key == GLFW.GLFW_KEY_TAB) {
            focusedField = switch (focusedField) {
                case NONE, PASS -> Field.HOST;
                case HOST -> Field.PORT;
                case PORT -> Field.USER;
                case USER -> Field.PASS;
            };
            return true;
        }

        if (key == GLFW.GLFW_KEY_ENTER) {
            saveConfig();
            GuiSounds.button();
            close();
            return true;
        }

        if (focusedField != Field.NONE) {
            if (ctrl && key == GLFW.GLFW_KEY_V) {
                String clip = GLFW.glfwGetClipboardString(win);
                if (clip != null && !clip.isEmpty()) {
                    clip = clip.trim().replace("\r", "").replace("\n", "");
                    switch (focusedField) {
                        case HOST -> {
                            if (clip.contains(":")) {
                                String[] parts = clip.split(":", 2);
                                String h = parts[0];
                                String p = parts[1].replaceAll("[^0-9]", "");
                                hostText = (hostText + h).substring(0, Math.min(128, hostText.length() + h.length()));
                                portText = p.substring(0, Math.min(5, p.length()));
                                focusedField = Field.PORT;
                            } else {
                                hostText = (hostText + clip).substring(0, Math.min(128, hostText.length() + clip.length()));
                            }
                        }
                        case PORT -> {
                            String digits = clip.replaceAll("[^0-9]", "");
                            portText = (portText + digits).substring(0, Math.min(5, portText.length() + digits.length()));
                        }
                        case USER -> userText = (userText + clip).substring(0, Math.min(64, userText.length() + clip.length()));
                        case PASS -> passText = (passText + clip).substring(0, Math.min(64, passText.length() + clip.length()));
                        default -> {}
                    }
                }
                return true;
            }

            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                switch (focusedField) {
                    case HOST -> { if (!hostText.isEmpty()) hostText = hostText.substring(0, hostText.length() - 1); }
                    case PORT -> { if (!portText.isEmpty()) portText = portText.substring(0, portText.length() - 1); }
                    case USER -> { if (!userText.isEmpty()) userText = userText.substring(0, userText.length() - 1); }
                    case PASS -> { if (!passText.isEmpty()) passText = passText.substring(0, passText.length() - 1); }
                    default -> {}
                }
                return true;
            }
        }

        return super.keyPressed(keyInput);
    }

    private void runProxyTest() {
        isTesting = true;
        lastTestResult = null;
        ProxyConfig testCfg = new ProxyConfig();
        testCfg.setType(selectedType);

        String host = hostText.trim();
        int port = 1080;
        if (host.contains(":") && !host.startsWith("[")) {
            String[] parts = host.split(":");
            host = parts[0].trim();
            try {
                port = Integer.parseInt(parts[1].trim());
                portText = String.valueOf(port);
            } catch (Exception ignored) {}
            hostText = host;
        } else {
            try {
                port = Integer.parseInt(portText.trim());
            } catch (Exception ignored) {}
        }

        testCfg.setHost(host);
        testCfg.setPort(port);
        testCfg.setUsername(userText.trim());
        testCfg.setPassword(passText);

        ProxyManager.testConnection(testCfg, res -> {
            isTesting = false;
            lastTestResult = res;
        });
    }

    private void saveConfig() {
        config.setEnabled(enabled);
        config.setType(selectedType);
        config.setLanguage(currentLang);

        String host = hostText.trim();
        int p = 1080;
        if (host.contains(":") && !host.startsWith("[")) {
            String[] parts = host.split(":");
            host = parts[0].trim();
            try {
                p = MathHelper.clamp(Integer.parseInt(parts[1].trim()), 1, 65535);
                portText = String.valueOf(p);
            } catch (Exception ignored) {}
            hostText = host;
        } else {
            try {
                p = MathHelper.clamp(Integer.parseInt(portText.trim()), 1, 65535);
            } catch (Exception ignored) {}
        }

        config.setHost(host);
        config.setPort(p);
        config.setUsername(userText.trim());
        config.setPassword(passText);
        config.save();
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
