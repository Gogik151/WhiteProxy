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

import java.util.List;

public class ProxyScreen extends Screen {

    private final Screen parent;
    private final ProxyConfig config;

    public enum ScreenTab { GENERAL, RULES }
    private ScreenTab currentTab = ScreenTab.GENERAL;

    private boolean enabled;
    private boolean hudEnabled;
    private boolean autoFailover;
    private boolean dnsLeakProtection;
    private ProxyConfig.Type selectedType;
    private String currentLang;

    private int selectedRuleIndex = 0;
    private String rulePatternText = "*.hypixel.net";
    private ProxyConfig.RuleAction ruleAction = ProxyConfig.RuleAction.DIRECT;
    private String ruleTargetProfile = "Default";
    private boolean ruleEnabled = true;

    private enum Field { NONE, HOST, PORT, USER, PASS, RULE_PATTERN }
    private Field focusedField = Field.NONE;

    private String hostText;
    private String portText;
    private String userText;
    private String passText;

    private ProxyManager.TestResult lastTestResult = null;
    private boolean isTesting = false;

    private final float winW = 340.0F;
    private final float winH = 208.0F;

    public ProxyScreen(Screen parent) {
        super(Text.literal("Proxy Settings"));
        this.parent = parent;
        this.config = ProxyManager.getConfig();
        this.enabled = config.isEnabled();
        this.hudEnabled = config.isHudEnabled();
        this.autoFailover = config.isAutoFailover();
        this.dnsLeakProtection = config.isDnsLeakProtection();
        this.selectedType = config.getType();
        this.currentLang = config.getLanguage();

        ProxyConfig.ProxyProfile profile = config.getActiveProfile();
        this.hostText = profile.getHost() != null ? profile.getHost() : "127.0.0.1";
        this.portText = String.valueOf(profile.getPort() > 0 ? profile.getPort() : 1080);
        this.userText = profile.getUsername() != null ? profile.getUsername() : "";
        this.passText = profile.getPassword() != null ? profile.getPassword() : "";

        if (!config.getServerRules().isEmpty()) {
            ProxyConfig.ServerRule r = config.getServerRules().get(0);
            this.rulePatternText = r.getPattern() != null ? r.getPattern() : "*.hypixel.net";
            this.ruleAction = r.getAction() != null ? r.getAction() : ProxyConfig.RuleAction.DIRECT;
            this.ruleTargetProfile = r.getTargetProfile() != null ? r.getTargetProfile() : "Default";
            this.ruleEnabled = r.isEnabled();
        }
    }

    private void syncCurrentToRule() {
        List<ProxyConfig.ServerRule> rules = config.getServerRules();
        if (selectedRuleIndex >= 0 && selectedRuleIndex < rules.size()) {
            ProxyConfig.ServerRule r = rules.get(selectedRuleIndex);
            r.setPattern(rulePatternText.trim());
            r.setAction(ruleAction);
            r.setTargetProfile(ruleTargetProfile);
            r.setEnabled(ruleEnabled);
        }
    }

    private void loadRule(int index) {
        syncCurrentToRule();
        List<ProxyConfig.ServerRule> rules = config.getServerRules();
        if (index >= 0 && index < rules.size()) {
            this.selectedRuleIndex = index;
            ProxyConfig.ServerRule r = rules.get(index);
            this.rulePatternText = r.getPattern() != null ? r.getPattern() : "";
            this.ruleAction = r.getAction() != null ? r.getAction() : ProxyConfig.RuleAction.DIRECT;
            this.ruleTargetProfile = r.getTargetProfile() != null ? r.getTargetProfile() : "Default";
            this.ruleEnabled = r.isEnabled();
        }
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

        // Кнопка закрытия X
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

        // Переключатель вкладок [ Общие | Правила ]
        float tabPillW = 86.0F;
        float tabPillH = 16.0F;
        float tabPillX = langX - 6.0F - tabPillW;
        float tabPillY = closeY;

        boolean isGenTab = (currentTab == ScreenTab.GENERAL);
        float halfTabW = tabPillW / 2.0F;
        boolean genTabHover = MathUtil.isHovered(mx, my, tabPillX, tabPillY, halfTabW, tabPillH);
        boolean rulesTabHover = MathUtil.isHovered(mx, my, tabPillX + halfTabW, tabPillY, halfTabW, tabPillH);

        Draw.rect(tabPillX, tabPillY, tabPillW, tabPillH, ColorUtil.getColor(15, 20, 32, 0.85F), 3.5F);
        Draw.outline(tabPillX, tabPillY, tabPillW, tabPillH, 1.0F, ColorUtil.getColor(36, 48, 70), 3.5F);

        if (isGenTab) {
            Draw.rect(tabPillX + 1.0F, tabPillY + 1.0F, halfTabW - 1.5F, tabPillH - 2.0F, accent, 2.5F);
            Fonts.sf_bold.drawCentered(I18n.tabGeneral(currentLang), tabPillX + halfTabW / 2.0F, tabPillY + 3.5F, 5.8F, ColorUtil.getColor(255));
        } else {
            int col = genTabHover ? ColorUtil.getColor(225, 235, 250) : ColorUtil.getColor(125, 135, 155);
            Fonts.sf_medium.drawCentered(I18n.tabGeneral(currentLang), tabPillX + halfTabW / 2.0F, tabPillY + 3.5F, 5.8F, col);
        }

        Draw.rect(tabPillX + halfTabW, tabPillY + 3.0F, 1.0F, tabPillH - 6.0F, ColorUtil.getColor(36, 48, 70));

        if (!isGenTab) {
            Draw.rect(tabPillX + halfTabW + 0.5F, tabPillY + 1.0F, halfTabW - 1.5F, tabPillH - 2.0F, accent, 2.5F);
            Fonts.sf_bold.drawCentered(I18n.tabRules(currentLang), tabPillX + halfTabW + halfTabW / 2.0F, tabPillY + 3.5F, 5.8F, ColorUtil.getColor(255));
        } else {
            int col = rulesTabHover ? ColorUtil.getColor(225, 235, 250) : ColorUtil.getColor(125, 135, 155);
            Fonts.sf_medium.drawCentered(I18n.tabRules(currentLang), tabPillX + halfTabW + halfTabW / 2.0F, tabPillY + 3.5F, 5.8F, col);
        }

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
        Draw.rect(winX, winY + 25.0F, winW, 1.0F, ColorUtil.getColor(28, 38, 56));

        if (currentTab == ScreenTab.GENERAL) {
            // 7. Панель профилей: [ < Profile 1/3 > ] [ + ] [ Del ] [ Import ]
            List<ProxyConfig.ProxyProfile> profiles = config.getProfiles();
            int activeIdx = config.getSelectedProfileIndex();
            float profY = winY + 30.0F;

            // Кнопка Предыдущий <
            float prevX = winX + 14.0F;
            float arrowBtnW = 15.0F;
            float arrowBtnH = 14.0F;
            boolean prevHov = MathUtil.isHovered(mx, my, prevX, profY, arrowBtnW, arrowBtnH);
            Draw.rect(prevX, profY, arrowBtnW, arrowBtnH, prevHov ? ColorUtil.getColor(26, 36, 52) : ColorUtil.getColor(18, 24, 36), 2.5F);
            Draw.outline(prevX, profY, arrowBtnW, arrowBtnH, 1.0F, ColorUtil.getColor(36, 48, 70), 2.5F);
            Fonts.sf_bold.drawCentered("<", prevX + arrowBtnW / 2.0F, profY + 2.5F, 6.8F, prevHov ? ColorUtil.getColor(255) : ColorUtil.getColor(160, 175, 200));

            // Плашка текущего профиля
            float pLabelX = prevX + arrowBtnW + 3.0F;
            float pLabelW = 86.0F;
            Draw.rect(pLabelX, profY, pLabelW, arrowBtnH, ColorUtil.getColor(14, 18, 28), 2.5F);
            Draw.outline(pLabelX, profY, pLabelW, arrowBtnH, 1.0F, ColorUtil.getColor(34, 46, 68), 2.5F);
            String profDisplay = "#" + (activeIdx + 1) + " " + (activeIdx < profiles.size() ? profiles.get(activeIdx).getName() : "");
            if (Fonts.sf_medium.getWidth(profDisplay, 6.2F) > pLabelW - 6.0F) {
                profDisplay = "#" + (activeIdx + 1) + " " + (activeIdx < profiles.size() ? profiles.get(activeIdx).getHost() : "");
            }
            Fonts.sf_medium.drawCentered(profDisplay, pLabelX + pLabelW / 2.0F, profY + 3.0F, 6.2F, ColorUtil.getColor(220, 230, 245));

            // Кнопка Следующий >
            float nextX = pLabelX + pLabelW + 3.0F;
            boolean nextHov = MathUtil.isHovered(mx, my, nextX, profY, arrowBtnW, arrowBtnH);
            Draw.rect(nextX, profY, arrowBtnW, arrowBtnH, nextHov ? ColorUtil.getColor(26, 36, 52) : ColorUtil.getColor(18, 24, 36), 2.5F);
            Draw.outline(nextX, profY, arrowBtnW, arrowBtnH, 1.0F, ColorUtil.getColor(36, 48, 70), 2.5F);
            Fonts.sf_bold.drawCentered(">", nextX + arrowBtnW / 2.0F, profY + 2.5F, 6.8F, nextHov ? ColorUtil.getColor(255) : ColorUtil.getColor(160, 175, 200));

            // Кнопка Добавить [ + ]
            float addX = nextX + arrowBtnW + 6.0F;
            float addW = 38.0F;
            boolean addHov = MathUtil.isHovered(mx, my, addX, profY, addW, arrowBtnH);
            Draw.rect(addX, profY, addW, arrowBtnH, addHov ? ColorUtil.replAlpha(accent, 0.35F) : ColorUtil.getColor(18, 26, 40), 2.5F);
            Draw.outline(addX, profY, addW, arrowBtnH, 1.0F, addHov ? accent : ColorUtil.getColor(38, 52, 76), 2.5F);
            Fonts.sf_medium.drawCentered(I18n.addProfile(currentLang), addX + addW / 2.0F, profY + 3.0F, 5.8F, addHov ? ColorUtil.getColor(255) : ColorUtil.getColor(190, 205, 230));

            // Кнопка Удалить [ Del ]
            float delX = addX + addW + 4.0F;
            float delW = 36.0F;
            boolean canDel = profiles.size() > 1;
            boolean delHov = canDel && MathUtil.isHovered(mx, my, delX, profY, delW, arrowBtnH);
            int delBg = delHov ? ColorUtil.getColor(240, 50, 50, 0.22F) : ColorUtil.getColor(18, 22, 32);
            int delBorder = delHov ? ColorUtil.getColor(245, 70, 70) : ColorUtil.getColor(32, 40, 58);
            Draw.rect(delX, profY, delW, arrowBtnH, delBg, 2.5F);
            Draw.outline(delX, profY, delW, arrowBtnH, 1.0F, delBorder, 2.5F);
            Fonts.sf_medium.drawCentered(I18n.delProfile(currentLang), delX + delW / 2.0F, profY + 3.0F, 5.8F,
                    canDel ? (delHov ? ColorUtil.getColor(255, 120, 120) : ColorUtil.getColor(160, 170, 190)) : ColorUtil.getColor(70, 80, 100));

            // Кнопка Импорт [ Import ]
            float impX = delX + delW + 4.0F;
            float impW = winX + winW - 14.0F - impX;
            boolean impHov = MathUtil.isHovered(mx, my, impX, profY, impW, arrowBtnH);
            Draw.rect(impX, profY, impW, arrowBtnH, impHov ? ColorUtil.getColor(28, 42, 64) : ColorUtil.getColor(18, 24, 36), 2.5F);
            Draw.outline(impX, profY, impW, arrowBtnH, 1.0F, impHov ? ColorUtil.getColor(64, 90, 135) : ColorUtil.getColor(36, 48, 70), 2.5F);
            Fonts.sf_medium.drawCentered(I18n.importClipboard(currentLang), impX + impW / 2.0F, profY + 3.0F, 5.8F,
                    impHov ? ColorUtil.getColor(245) : ColorUtil.getColor(180, 195, 220));

            // 8. Переключатели: [Включить] + [HUD] + [DNS Guard] + [Failover]
            float optsY = winY + 49.0F;
            float switchW = 18.0F;
            float switchH = 11.0F;
            float switchX = winX + 14.0F;

            int trackCol = enabled ? accent : ColorUtil.getColor(22, 30, 44);
            Draw.rect(switchX, optsY + 1.0F, switchW, switchH, trackCol, switchH / 2.0F);
            Draw.outline(switchX, optsY + 1.0F, switchW, switchH, 1.0F, enabled ? accent : ColorUtil.getColor(44, 58, 82), switchH / 2.0F);
            float thumbSize = switchH - 2.5F;
            float thumbX = switchX + 1.25F + (enabled ? (switchW - thumbSize - 2.5F) : 0.0F);
            Draw.rect(thumbX, optsY + 2.25F, thumbSize, thumbSize, ColorUtil.getColor(255), thumbSize / 2.0F);

            Fonts.sf_medium.draw(I18n.useProxy(currentLang), switchX + switchW + 5.0F, optsY + 2.5F, 6.2F,
                    enabled ? ColorUtil.getColor(255) : ColorUtil.getColor(170, 180, 200));

            // HUD Toggle Badge
            float hudBadgeX = winX + 124.0F;
            float hudBadgeW = 56.0F;
            float badgeH = 13.0F;
            boolean hudHov = MathUtil.isHovered(mx, my, hudBadgeX, optsY, hudBadgeW, badgeH);
            int hudBg = hudEnabled ? ColorUtil.replAlpha(accent, 0.25F) : (hudHov ? ColorUtil.getColor(20, 26, 38) : ColorUtil.getColor(14, 18, 28));
            int hudBorder = hudEnabled ? accent : (hudHov ? ColorUtil.getColor(50, 68, 98) : ColorUtil.getColor(30, 40, 60));
            Draw.rect(hudBadgeX, optsY, hudBadgeW, badgeH, hudBg, 2.5F);
            Draw.outline(hudBadgeX, optsY, hudBadgeW, badgeH, 1.0F, hudBorder, 2.5F);
            String hudText = (hudEnabled ? "● " : "○ ") + I18n.hudOption(currentLang);
            Fonts.sf_medium.drawCentered(hudText, hudBadgeX + hudBadgeW / 2.0F, optsY + 2.5F, 5.5F,
                    hudEnabled ? ColorUtil.getColor(245) : ColorUtil.getColor(140, 150, 170));

            // DNS Leak Protection Badge
            float dnsBadgeX = hudBadgeX + hudBadgeW + 4.0F;
            float dnsBadgeW = 68.0F;
            boolean dnsHov = MathUtil.isHovered(mx, my, dnsBadgeX, optsY, dnsBadgeW, badgeH);
            int dnsBg = dnsLeakProtection ? ColorUtil.getColor(0, 195, 230, 0.22F) : (dnsHov ? ColorUtil.getColor(20, 26, 38) : ColorUtil.getColor(14, 18, 28));
            int dnsBorder = dnsLeakProtection ? ColorUtil.getColor(0, 215, 255, 0.85F) : (dnsHov ? ColorUtil.getColor(50, 68, 98) : ColorUtil.getColor(30, 40, 60));
            Draw.rect(dnsBadgeX, optsY, dnsBadgeW, badgeH, dnsBg, 2.5F);
            Draw.outline(dnsBadgeX, optsY, dnsBadgeW, badgeH, 1.0F, dnsBorder, 2.5F);
            String dnsText = (dnsLeakProtection ? "● " : "○ ") + I18n.dnsOption(currentLang);
            Fonts.sf_medium.drawCentered(dnsText, dnsBadgeX + dnsBadgeW / 2.0F, optsY + 2.5F, 5.3F,
                    dnsLeakProtection ? ColorUtil.getColor(100, 235, 255) : ColorUtil.getColor(140, 150, 170));

            // Failover Toggle Badge
            float failBadgeX = dnsBadgeX + dnsBadgeW + 4.0F;
            float failBadgeW = winX + winW - 14.0F - failBadgeX;
            boolean failHov = MathUtil.isHovered(mx, my, failBadgeX, optsY, failBadgeW, badgeH);
            int failBg = autoFailover ? ColorUtil.getColor(50, 205, 120, 0.22F) : (failHov ? ColorUtil.getColor(20, 26, 38) : ColorUtil.getColor(14, 18, 28));
            int failBorder = autoFailover ? ColorUtil.getColor(50, 205, 120, 0.8F) : (failHov ? ColorUtil.getColor(50, 68, 98) : ColorUtil.getColor(30, 40, 60));
            Draw.rect(failBadgeX, optsY, failBadgeW, badgeH, failBg, 2.5F);
            Draw.outline(failBadgeX, optsY, failBadgeW, badgeH, 1.0F, failBorder, 2.5F);
            String failText = (autoFailover ? "● " : "○ ") + I18n.failoverOption(currentLang);
            Fonts.sf_medium.drawCentered(failText, failBadgeX + failBadgeW / 2.0F, optsY + 2.5F, 5.3F,
                    autoFailover ? ColorUtil.getColor(120, 255, 170) : ColorUtil.getColor(140, 150, 170));

            // 9. Выбор протокола (SOCKS5 / SOCKS4 / HTTP)
            ProxyConfig.Type[] types = ProxyConfig.Type.values();
            float tabGap = 6.0F;
            float tabW = (winW - 28.0F - (types.length - 1) * tabGap) / types.length;
            float tabH = 15.0F;
            float tabY = winY + 68.0F;

            for (int i = 0; i < types.length; i++) {
                ProxyConfig.Type t = types[i];
                float tabX = winX + 14.0F + i * (tabW + tabGap);
                boolean isSel = (selectedType == t);
                boolean isHov = MathUtil.isHovered(mx, my, tabX, tabY, tabW, tabH);

                int tBg = isSel ? ColorUtil.replAlpha(accent, 0.28F) : (isHov ? ColorUtil.getColor(20, 28, 42) : ColorUtil.getColor(14, 18, 28));
                int tBorder = isSel ? accent : (isHov ? ColorUtil.getColor(54, 72, 104) : ColorUtil.getColor(28, 38, 54));

                Draw.rect(tabX, tabY, tabW, tabH, tBg, 3.0F);
                Draw.outline(tabX, tabY, tabW, tabH, 1.0F, tBorder, 3.0F);
                Fonts.sf_bold.drawCentered(t.getDisplayName(), tabX + tabW / 2.0F, tabY + 3.5F, 6.5F,
                        isSel ? ColorUtil.getColor(255) : (isHov ? ColorUtil.getColor(215, 225, 240) : ColorUtil.getColor(145, 155, 175)));
            }

            // 10. Поля ввода Host и Port
            float hostH = 18.0F;
            float hostY = winY + 95.0F;
            float portW = 58.0F;
            float hostW = winW - 28.0F - portW - 8.0F;
            float hostX = winX + 14.0F;
            float portX = hostX + hostW + 8.0F;

            drawInputField(hostX, hostY, hostW, hostH, I18n.hostLabel(currentLang), hostText, "127.0.0.1", false, focusedField == Field.HOST, mx, my);
            drawInputField(portX, hostY, portW, hostH, I18n.portLabel(currentLang), portText, "1080", false, focusedField == Field.PORT, mx, my);

            // 11. Поля ввода Login и Password
            float credsY = winY + 125.0F;
            float credsW = (winW - 28.0F - 8.0F) / 2.0F;
            float userX = winX + 14.0F;
            float passX = userX + credsW + 8.0F;

            drawInputField(userX, credsY, credsW, hostH, I18n.userLabel(currentLang), userText, I18n.userPlaceholder(currentLang), false, focusedField == Field.USER, mx, my);
            drawInputField(passX, credsY, credsW, hostH, I18n.passLabel(currentLang), passText, I18n.passPlaceholder(currentLang), true, focusedField == Field.PASS, mx, my);

            // 12. Статус проверки подключения и текущий пинг
            float statusY = winY + 151.0F;
            if (isTesting) {
                Fonts.sf_medium.draw(I18n.testing(currentLang), winX + 14.0F, statusY, 6.5F, ColorUtil.getColor(245, 195, 60));
            } else if (lastTestResult != null) {
                int statCol = lastTestResult.success() ? ColorUtil.getColor(50, 225, 110) : ColorUtil.getColor(245, 80, 80);
                Draw.rect(winX + 14.0F, statusY + 2.0F, 4.0F, 4.0F, statCol, 2.0F);

                String statusStr = lastTestResult.success()
                        ? I18n.success(currentLang, lastTestResult.pingMs())
                        : I18n.failed(currentLang, lastTestResult.error());
                Fonts.sf_medium.draw(statusStr, winX + 22.0F, statusY, 6.5F, statCol);
            } else {
                ProxyConfig.ProxyProfile p = config.getActiveProfile();
                if (p != null && p.getLastPingMs() > 0) {
                    int pingCol = p.getLastPingMs() < 100 ? ColorUtil.getColor(50, 225, 110)
                            : (p.getLastPingMs() < 250 ? ColorUtil.getColor(245, 200, 60) : ColorUtil.getColor(245, 80, 80));
                    Draw.rect(winX + 14.0F, statusY + 2.0F, 4.0F, 4.0F, pingCol, 2.0F);
                    Fonts.sf_medium.draw("Latency: " + p.getLastPingMs() + " ms", winX + 22.0F, statusY, 6.2F, pingCol);
                }
            }
        } else {
            // Вкладка ПРАВИЛА МАРШРУТИЗАЦИИ
            List<ProxyConfig.ServerRule> rules = config.getServerRules();
            float rBarY = winY + 30.0F;
            float arrowBtnW = 15.0F;
            float arrowBtnH = 14.0F;

            if (rules.isEmpty()) {
                float addRuleW = 68.0F;
                float addRuleX = winX + 14.0F;
                boolean addRuleHov = MathUtil.isHovered(mx, my, addRuleX, rBarY, addRuleW, arrowBtnH);
                Draw.rect(addRuleX, rBarY, addRuleW, arrowBtnH, addRuleHov ? ColorUtil.replAlpha(accent, 0.35F) : ColorUtil.getColor(18, 26, 40), 2.5F);
                Draw.outline(addRuleX, rBarY, addRuleW, arrowBtnH, 1.0F, addRuleHov ? accent : ColorUtil.getColor(38, 52, 76), 2.5F);
                Fonts.sf_medium.drawCentered(I18n.addRule(currentLang), addRuleX + addRuleW / 2.0F, rBarY + 3.0F, 5.8F, addRuleHov ? ColorUtil.getColor(255) : ColorUtil.getColor(190, 205, 230));

                Fonts.sf_medium.draw(I18n.noRules(currentLang), addRuleX + addRuleW + 8.0F, rBarY + 3.0F, 6.0F, ColorUtil.getColor(140, 150, 170));
            } else {
                // Кнопка <
                float prevRX = winX + 14.0F;
                boolean prevRHov = MathUtil.isHovered(mx, my, prevRX, rBarY, arrowBtnW, arrowBtnH);
                Draw.rect(prevRX, rBarY, arrowBtnW, arrowBtnH, prevRHov ? ColorUtil.getColor(26, 36, 52) : ColorUtil.getColor(18, 24, 36), 2.5F);
                Draw.outline(prevRX, rBarY, arrowBtnW, arrowBtnH, 1.0F, ColorUtil.getColor(36, 48, 70), 2.5F);
                Fonts.sf_bold.drawCentered("<", prevRX + arrowBtnW / 2.0F, rBarY + 2.5F, 6.8F, prevRHov ? ColorUtil.getColor(255) : ColorUtil.getColor(160, 175, 200));

                // Плашка номера правила
                float rLabelX = prevRX + arrowBtnW + 3.0F;
                float rLabelW = 86.0F;
                Draw.rect(rLabelX, rBarY, rLabelW, arrowBtnH, ColorUtil.getColor(14, 18, 28), 2.5F);
                Draw.outline(rLabelX, rBarY, rLabelW, arrowBtnH, 1.0F, ColorUtil.getColor(34, 46, 68), 2.5F);
                String rDisplay = "Rule #" + (selectedRuleIndex + 1) + "/" + rules.size();
                Fonts.sf_medium.drawCentered(rDisplay, rLabelX + rLabelW / 2.0F, rBarY + 3.0F, 6.2F, ColorUtil.getColor(220, 230, 245));

                // Кнопка >
                float nextRX = rLabelX + rLabelW + 3.0F;
                boolean nextRHov = MathUtil.isHovered(mx, my, nextRX, rBarY, arrowBtnW, arrowBtnH);
                Draw.rect(nextRX, rBarY, arrowBtnW, arrowBtnH, nextRHov ? ColorUtil.getColor(26, 36, 52) : ColorUtil.getColor(18, 24, 36), 2.5F);
                Draw.outline(nextRX, rBarY, arrowBtnW, arrowBtnH, 1.0F, ColorUtil.getColor(36, 48, 70), 2.5F);
                Fonts.sf_bold.drawCentered(">", nextRX + arrowBtnW / 2.0F, rBarY + 2.5F, 6.8F, nextRHov ? ColorUtil.getColor(255) : ColorUtil.getColor(160, 175, 200));

                // Кнопка + Правило
                float addRX = nextRX + arrowBtnW + 6.0F;
                float addRW = 46.0F;
                boolean addRHov = MathUtil.isHovered(mx, my, addRX, rBarY, addRW, arrowBtnH);
                Draw.rect(addRX, rBarY, addRW, arrowBtnH, addRHov ? ColorUtil.replAlpha(accent, 0.35F) : ColorUtil.getColor(18, 26, 40), 2.5F);
                Draw.outline(addRX, rBarY, addRW, arrowBtnH, 1.0F, addRHov ? accent : ColorUtil.getColor(38, 52, 76), 2.5F);
                Fonts.sf_medium.drawCentered(I18n.addRule(currentLang), addRX + addRW / 2.0F, rBarY + 3.0F, 5.8F, addRHov ? ColorUtil.getColor(255) : ColorUtil.getColor(190, 205, 230));

                // Кнопка Del
                float delRX = addRX + addRW + 4.0F;
                float delRW = 36.0F;
                boolean delRHov = MathUtil.isHovered(mx, my, delRX, rBarY, delRW, arrowBtnH);
                int delBg = delRHov ? ColorUtil.getColor(240, 50, 50, 0.22F) : ColorUtil.getColor(18, 22, 32);
                int delBorder = delRHov ? ColorUtil.getColor(245, 70, 70) : ColorUtil.getColor(32, 40, 58);
                Draw.rect(delRX, rBarY, delRW, arrowBtnH, delBg, 2.5F);
                Draw.outline(delRX, rBarY, delRW, arrowBtnH, 1.0F, delBorder, 2.5F);
                Fonts.sf_medium.drawCentered(I18n.delRule(currentLang), delRX + delRW / 2.0F, rBarY + 3.0F, 5.8F,
                        delRHov ? ColorUtil.getColor(255, 120, 120) : ColorUtil.getColor(160, 170, 190));

                // Бейдж активности
                float rActiveX = delRX + delRW + 6.0F;
                float rActiveW = winX + winW - 14.0F - rActiveX;
                boolean rActiveHov = MathUtil.isHovered(mx, my, rActiveX, rBarY, rActiveW, arrowBtnH);
                int rActBg = ruleEnabled ? ColorUtil.getColor(50, 205, 120, 0.22F) : (rActiveHov ? ColorUtil.getColor(20, 26, 38) : ColorUtil.getColor(14, 18, 28));
                int rActBorder = ruleEnabled ? ColorUtil.getColor(50, 205, 120, 0.8F) : (rActiveHov ? ColorUtil.getColor(50, 68, 98) : ColorUtil.getColor(30, 40, 60));
                Draw.rect(rActiveX, rBarY, rActiveW, arrowBtnH, rActBg, 2.5F);
                Draw.outline(rActiveX, rBarY, rActiveW, arrowBtnH, 1.0F, rActBorder, 2.5F);
                String rActText = (ruleEnabled ? "● " : "○ ") + (ruleEnabled ? "Active" : "Disabled");
                Fonts.sf_medium.drawCentered(rActText, rActiveX + rActiveW / 2.0F, rBarY + 3.0F, 5.8F,
                        ruleEnabled ? ColorUtil.getColor(120, 255, 170) : ColorUtil.getColor(140, 150, 170));
            }

            // Поле ввода шаблона сервера
            float rPatY = winY + 58.0F;
            float rPatW = winW - 28.0F;
            drawInputField(winX + 14.0F, rPatY, rPatW, 18.0F, I18n.rulePatternLabel(currentLang), rulePatternText, "*.hypixel.net", false, focusedField == Field.RULE_PATTERN, mx, my);

            // Выбор действия [ Напрямую (Direct) ] vs [ Через Прокси (Proxy) ]
            float rActTabsY = winY + 92.0F;
            Fonts.sf_regular.draw(I18n.ruleActionLabel(currentLang), winX + 14.0F, rActTabsY - 8.5F, 5.8F, ColorUtil.getColor(130, 140, 162));

            float actBtnW = (winW - 28.0F - 6.0F) / 2.0F;
            float actBtnH = 16.0F;

            // Кнопка Напрямую
            boolean isDirect = (ruleAction == ProxyConfig.RuleAction.DIRECT);
            boolean dirHov = MathUtil.isHovered(mx, my, winX + 14.0F, rActTabsY, actBtnW, actBtnH);
            int dirBg = isDirect ? ColorUtil.getColor(50, 205, 120, 0.25F) : (dirHov ? ColorUtil.getColor(20, 28, 42) : ColorUtil.getColor(14, 18, 28));
            int dirBorder = isDirect ? ColorUtil.getColor(50, 205, 120) : (dirHov ? ColorUtil.getColor(54, 72, 104) : ColorUtil.getColor(28, 38, 54));
            Draw.rect(winX + 14.0F, rActTabsY, actBtnW, actBtnH, dirBg, 3.0F);
            Draw.outline(winX + 14.0F, rActTabsY, actBtnW, actBtnH, 1.0F, dirBorder, 3.0F);
            Fonts.sf_bold.drawCentered("⚡ " + I18n.ruleDirect(currentLang), winX + 14.0F + actBtnW / 2.0F, rActTabsY + 4.0F, 6.2F,
                    isDirect ? ColorUtil.getColor(255) : ColorUtil.getColor(155, 165, 185));

            // Кнопка Прокси
            boolean isProf = (ruleAction == ProxyConfig.RuleAction.PROFILE);
            float profTabX = winX + 14.0F + actBtnW + 6.0F;
            boolean profTabHov = MathUtil.isHovered(mx, my, profTabX, rActTabsY, actBtnW, actBtnH);
            int profBg = isProf ? ColorUtil.replAlpha(accent, 0.28F) : (profTabHov ? ColorUtil.getColor(20, 28, 42) : ColorUtil.getColor(14, 18, 28));
            int profBorder = isProf ? accent : (profTabHov ? ColorUtil.getColor(54, 72, 104) : ColorUtil.getColor(28, 38, 54));
            Draw.rect(profTabX, rActTabsY, actBtnW, actBtnH, profBg, 3.0F);
            Draw.outline(profTabX, rActTabsY, actBtnW, actBtnH, 1.0F, profBorder, 3.0F);
            Fonts.sf_bold.drawCentered("🛡️ " + I18n.ruleProfile(currentLang), profTabX + actBtnW / 2.0F, rActTabsY + 4.0F, 6.2F,
                    isProf ? ColorUtil.getColor(255) : ColorUtil.getColor(155, 165, 185));

            // Если выбрано действие Прокси — выбор целевого профиля
            if (ruleAction == ProxyConfig.RuleAction.PROFILE) {
                float profSelY = winY + 120.0F;
                Fonts.sf_regular.draw(I18n.profileLabel(currentLang), winX + 14.0F, profSelY - 8.5F, 5.8F, ColorUtil.getColor(130, 140, 162));

                float prevPX = winX + 14.0F;
                boolean prevPHov = MathUtil.isHovered(mx, my, prevPX, profSelY, arrowBtnW, arrowBtnH);
                Draw.rect(prevPX, profSelY, arrowBtnW, arrowBtnH, prevPHov ? ColorUtil.getColor(26, 36, 52) : ColorUtil.getColor(18, 24, 36), 2.5F);
                Draw.outline(prevPX, profSelY, arrowBtnW, arrowBtnH, 1.0F, ColorUtil.getColor(36, 48, 70), 2.5F);
                Fonts.sf_bold.drawCentered("<", prevPX + arrowBtnW / 2.0F, profSelY + 2.5F, 6.8F, prevPHov ? ColorUtil.getColor(255) : ColorUtil.getColor(160, 175, 200));

                float profBoxX = prevPX + arrowBtnW + 3.0F;
                float profBoxW = winW - 28.0F - (arrowBtnW + 3.0F) * 2.0F;
                Draw.rect(profBoxX, profSelY, profBoxW, arrowBtnH, ColorUtil.getColor(14, 18, 28), 2.5F);
                Draw.outline(profBoxX, profSelY, profBoxW, arrowBtnH, 1.0F, ColorUtil.getColor(34, 46, 68), 2.5F);
                Fonts.sf_medium.drawCentered("Target Profile: " + ruleTargetProfile, profBoxX + profBoxW / 2.0F, profSelY + 3.0F, 6.2F, ColorUtil.getColor(220, 230, 245));

                float nextPX = profBoxX + profBoxW + 3.0F;
                boolean nextPHov = MathUtil.isHovered(mx, my, nextPX, profSelY, arrowBtnW, arrowBtnH);
                Draw.rect(nextPX, profSelY, arrowBtnW, arrowBtnH, nextPHov ? ColorUtil.getColor(26, 36, 52) : ColorUtil.getColor(18, 24, 36), 2.5F);
                Draw.outline(nextPX, profSelY, arrowBtnW, arrowBtnH, 1.0F, ColorUtil.getColor(36, 48, 70), 2.5F);
                Fonts.sf_bold.drawCentered(">", nextPX + arrowBtnW / 2.0F, profSelY + 2.5F, 6.8F, nextPHov ? ColorUtil.getColor(255) : ColorUtil.getColor(160, 175, 200));

                float tipY = winY + 144.0F;
                Fonts.sf_medium.draw("ℹ Auto-routes matching hosts through " + ruleTargetProfile, winX + 14.0F, tipY, 5.8F, ColorUtil.getColor(120, 140, 175));
            } else {
                float tipY = winY + 118.0F;
                Fonts.sf_medium.draw("ℹ Matching servers bypass all proxies and connect directly.", winX + 14.0F, tipY, 5.8F, ColorUtil.getColor(120, 200, 150));
            }
        }

        // 13. Кнопки в подвале
        float btnH = 20.0F;
        float btnY = winY + 172.0F;

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

    private void syncCurrentToProfile() {
        ProxyConfig.ProxyProfile p = config.getActiveProfile();
        if (p != null) {
            p.setType(selectedType);
            String h = hostText.trim();
            p.setHost(h);
            try {
                p.setPort(MathHelper.clamp(Integer.parseInt(portText.trim()), 1, 65535));
            } catch (Exception ignored) {}
            p.setUsername(userText.trim());
            p.setPassword(passText);
        }
    }

    private void loadProfile(int index) {
        syncCurrentToProfile();
        config.setSelectedProfileIndex(index);
        ProxyConfig.ProxyProfile p = config.getActiveProfile();
        if (p != null) {
            this.selectedType = p.getType();
            this.hostText = p.getHost() != null ? p.getHost() : "127.0.0.1";
            this.portText = String.valueOf(p.getPort() > 0 ? p.getPort() : 1080);
            this.userText = p.getUsername() != null ? p.getUsername() : "";
            this.passText = p.getPassword() != null ? p.getPassword() : "";
            this.lastTestResult = null;
        }
    }

    private void importFromClipboard(long win) {
        String clip = GLFW.glfwGetClipboardString(win);
        if (clip != null && !clip.trim().isEmpty()) {
            ProxyConfig.ProxyProfile parsed = ProxyConfig.ProxyProfile.parse(clip);
            if (parsed != null) {
                this.selectedType = parsed.getType();
                this.hostText = parsed.getHost();
                this.portText = String.valueOf(parsed.getPort());
                this.userText = parsed.getUsername();
                this.passText = parsed.getPassword();
                syncCurrentToProfile();
                config.save();
                GuiSounds.button();
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

            // Переключатель вкладок [ Общие | Правила ]
            float tabPillW = 86.0F;
            float tabPillH = 16.0F;
            float tabPillX = closeX - 6.0F - langW - 6.0F - tabPillW;
            float tabPillY = closeY;
            float halfTabW = tabPillW / 2.0F;

            if (MathUtil.isHovered(mx, my, tabPillX, tabPillY, halfTabW, tabPillH)) {
                if (currentTab != ScreenTab.GENERAL) {
                    currentTab = ScreenTab.GENERAL;
                    focusedField = Field.NONE;
                    GuiSounds.button();
                }
                return true;
            } else if (MathUtil.isHovered(mx, my, tabPillX + halfTabW, tabPillY, halfTabW, tabPillH)) {
                if (currentTab != ScreenTab.RULES) {
                    currentTab = ScreenTab.RULES;
                    focusedField = Field.NONE;
                    GuiSounds.button();
                }
                return true;
            }

            if (currentTab == ScreenTab.GENERAL) {
                // 7. Панель профилей кнопки
                float profY = winY + 30.0F;
                float prevX = winX + 14.0F;
                float arrowBtnW = 15.0F;
                float arrowBtnH = 14.0F;

                // Кнопка Предыдущий <
                if (MathUtil.isHovered(mx, my, prevX, profY, arrowBtnW, arrowBtnH)) {
                    int total = config.getProfiles().size();
                    int prevIdx = (config.getSelectedProfileIndex() - 1 + total) % total;
                    loadProfile(prevIdx);
                    GuiSounds.picker(true);
                    return true;
                }

                // Кнопка Следующий >
                float pLabelX = prevX + arrowBtnW + 3.0F;
                float pLabelW = 86.0F;
                float nextX = pLabelX + pLabelW + 3.0F;
                if (MathUtil.isHovered(mx, my, nextX, profY, arrowBtnW, arrowBtnH)) {
                    int total = config.getProfiles().size();
                    int nextIdx = (config.getSelectedProfileIndex() + 1) % total;
                    loadProfile(nextIdx);
                    GuiSounds.picker(true);
                    return true;
                }

                // Кнопка Добавить [ + ]
                float addX = nextX + arrowBtnW + 6.0F;
                float addW = 38.0F;
                if (MathUtil.isHovered(mx, my, addX, profY, addW, arrowBtnH)) {
                    syncCurrentToProfile();
                    int newNum = config.getProfiles().size() + 1;
                    ProxyConfig.ProxyProfile np = new ProxyConfig.ProxyProfile("Profile " + newNum, ProxyConfig.Type.SOCKS5, "127.0.0.1", 1080, "", "");
                    config.getProfiles().add(np);
                    loadProfile(config.getProfiles().size() - 1);
                    config.save();
                    GuiSounds.button();
                    return true;
                }

                // Кнопка Удалить [ Del ]
                float delX = addX + addW + 4.0F;
                float delW = 36.0F;
                if (config.getProfiles().size() > 1 && MathUtil.isHovered(mx, my, delX, profY, delW, arrowBtnH)) {
                    int idx = config.getSelectedProfileIndex();
                    config.getProfiles().remove(idx);
                    int nextIdx = Math.max(0, idx - 1);
                    config.setSelectedProfileIndex(nextIdx);
                    loadProfile(nextIdx);
                    config.save();
                    GuiSounds.button();
                    return true;
                }

                // Кнопка Импорт [ Import ]
                float impX = delX + delW + 4.0F;
                float impW = winX + winW - 14.0F - impX;
                if (MathUtil.isHovered(mx, my, impX, profY, impW, arrowBtnH)) {
                    long win = mc.getWindow().getHandle();
                    importFromClipboard(win);
                    return true;
                }

                // 8. Переключатели: [Включить] + [HUD] + [DNS Guard] + [Failover]
                float optsY = winY + 49.0F;
                float switchW = 18.0F;
                float switchH = 11.0F;
                float switchX = winX + 14.0F;
                if (MathUtil.isHovered(mx, my, switchX - 2.0F, optsY - 2.0F, switchW + 90.0F, switchH + 4.0F)) {
                    enabled = !enabled;
                    GuiSounds.toggle(enabled);
                    return true;
                }

                // HUD Toggle Badge
                float hudBadgeX = winX + 124.0F;
                float hudBadgeW = 56.0F;
                float badgeH = 13.0F;
                if (MathUtil.isHovered(mx, my, hudBadgeX, optsY, hudBadgeW, badgeH)) {
                    hudEnabled = !hudEnabled;
                    GuiSounds.toggle(hudEnabled);
                    return true;
                }

                // DNS Leak Protection Badge
                float dnsBadgeX = hudBadgeX + hudBadgeW + 4.0F;
                float dnsBadgeW = 68.0F;
                if (MathUtil.isHovered(mx, my, dnsBadgeX, optsY, dnsBadgeW, badgeH)) {
                    dnsLeakProtection = !dnsLeakProtection;
                    GuiSounds.toggle(dnsLeakProtection);
                    return true;
                }

                // Failover Toggle Badge
                float failBadgeX = dnsBadgeX + dnsBadgeW + 4.0F;
                float failBadgeW = winX + winW - 14.0F - failBadgeX;
                if (MathUtil.isHovered(mx, my, failBadgeX, optsY, failBadgeW, badgeH)) {
                    autoFailover = !autoFailover;
                    GuiSounds.toggle(autoFailover);
                    return true;
                }

                // 9. Табы протокола
                ProxyConfig.Type[] types = ProxyConfig.Type.values();
                float tabGap = 6.0F;
                float tabW = (winW - 28.0F - (types.length - 1) * tabGap) / types.length;
                float tabH = 15.0F;
                float tabY = winY + 68.0F;

                for (int i = 0; i < types.length; i++) {
                    float tabX = winX + 14.0F + i * (tabW + tabGap);
                    if (MathUtil.isHovered(mx, my, tabX, tabY, tabW, tabH)) {
                        selectedType = types[i];
                        GuiSounds.picker(true);
                        return true;
                    }
                }

                // 10. Поля ввода
                float hostH = 18.0F;
                float hostY = winY + 95.0F;
                float portW = 58.0F;
                float hostW = winW - 28.0F - portW - 8.0F;
                float hostX = winX + 14.0F;
                float portX = hostX + hostW + 8.0F;

                float credsY = winY + 125.0F;
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

                // [ Проверить / Check ]
                float btnH = 20.0F;
                float btnY = winY + 172.0F;
                float testBtnW = 76.0F;
                float testBtnX = winX + 14.0F;
                if (MathUtil.isHovered(mx, my, testBtnX, btnY, testBtnW, btnH)) {
                    runProxyTest();
                    GuiSounds.button();
                    return true;
                }
            } else {
                // Вкладка RULES клики
                List<ProxyConfig.ServerRule> rules = config.getServerRules();
                float rBarY = winY + 30.0F;
                float arrowBtnW = 15.0F;
                float arrowBtnH = 14.0F;

                if (rules.isEmpty()) {
                    if (MathUtil.isHovered(mx, my, winX + 14.0F, rBarY, 68.0F, arrowBtnH)) {
                        rules.add(new ProxyConfig.ServerRule("*.example.com", ProxyConfig.RuleAction.DIRECT, "Default", true));
                        loadRule(0);
                        GuiSounds.button();
                        return true;
                    }
                } else {
                    // Кнопка <
                    float prevRX = winX + 14.0F;
                    if (MathUtil.isHovered(mx, my, prevRX, rBarY, arrowBtnW, arrowBtnH)) {
                        int prevR = (selectedRuleIndex - 1 + rules.size()) % rules.size();
                        loadRule(prevR);
                        GuiSounds.picker(true);
                        return true;
                    }

                    // Кнопка >
                    float rLabelX = prevRX + arrowBtnW + 3.0F;
                    float rLabelW = 86.0F;
                    float nextRX = rLabelX + rLabelW + 3.0F;
                    if (MathUtil.isHovered(mx, my, nextRX, rBarY, arrowBtnW, arrowBtnH)) {
                        int nextR = (selectedRuleIndex + 1) % rules.size();
                        loadRule(nextR);
                        GuiSounds.picker(true);
                        return true;
                    }

                    // Кнопка +
                    float addRX = nextRX + arrowBtnW + 6.0F;
                    float addRW = 46.0F;
                    if (MathUtil.isHovered(mx, my, addRX, rBarY, addRW, arrowBtnH)) {
                        syncCurrentToRule();
                        rules.add(new ProxyConfig.ServerRule("*.server" + (rules.size() + 1) + ".net", ProxyConfig.RuleAction.DIRECT, "Default", true));
                        loadRule(rules.size() - 1);
                        GuiSounds.button();
                        return true;
                    }

                    // Кнопка Del
                    float delRX = addRX + addRW + 4.0F;
                    float delRW = 36.0F;
                    if (MathUtil.isHovered(mx, my, delRX, rBarY, delRW, arrowBtnH)) {
                        rules.remove(selectedRuleIndex);
                        int nextR = Math.max(0, selectedRuleIndex - 1);
                        if (!rules.isEmpty()) {
                            loadRule(nextR);
                        } else {
                            rulePatternText = "*.hypixel.net";
                            ruleAction = ProxyConfig.RuleAction.DIRECT;
                            ruleTargetProfile = "Default";
                            ruleEnabled = true;
                        }
                        GuiSounds.button();
                        return true;
                    }

                    // Бейдж активности правила
                    float rActiveX = delRX + delRW + 6.0F;
                    float rActiveW = winX + winW - 14.0F - rActiveX;
                    if (MathUtil.isHovered(mx, my, rActiveX, rBarY, rActiveW, arrowBtnH)) {
                        ruleEnabled = !ruleEnabled;
                        syncCurrentToRule();
                        GuiSounds.toggle(ruleEnabled);
                        return true;
                    }
                }

                // Поле ввода шаблона
                float rPatY = winY + 58.0F;
                if (MathUtil.isHovered(mx, my, winX + 14.0F, rPatY, winW - 28.0F, 18.0F)) {
                    focusedField = Field.RULE_PATTERN;
                    return true;
                } else {
                    focusedField = Field.NONE;
                }

                // Выбор действия (Напрямую vs Прокси)
                float rActTabsY = winY + 92.0F;
                float actBtnW = (winW - 28.0F - 6.0F) / 2.0F;
                float actBtnH = 16.0F;
                if (MathUtil.isHovered(mx, my, winX + 14.0F, rActTabsY, actBtnW, actBtnH)) {
                    ruleAction = ProxyConfig.RuleAction.DIRECT;
                    syncCurrentToRule();
                    GuiSounds.picker(true);
                    return true;
                }

                float profTabX = winX + 14.0F + actBtnW + 6.0F;
                if (MathUtil.isHovered(mx, my, profTabX, rActTabsY, actBtnW, actBtnH)) {
                    ruleAction = ProxyConfig.RuleAction.PROFILE;
                    syncCurrentToRule();
                    GuiSounds.picker(true);
                    return true;
                }

                // Выбор целевого профиля
                if (ruleAction == ProxyConfig.RuleAction.PROFILE) {
                    float profSelY = winY + 120.0F;
                    float prevPX = winX + 14.0F;
                    if (MathUtil.isHovered(mx, my, prevPX, profSelY, arrowBtnW, arrowBtnH)) {
                        cycleRuleProfile(-1);
                        return true;
                    }
                    float profBoxX = prevPX + arrowBtnW + 3.0F;
                    float profBoxW = winW - 28.0F - (arrowBtnW + 3.0F) * 2.0F;
                    float nextPX = profBoxX + profBoxW + 3.0F;
                    if (MathUtil.isHovered(mx, my, nextPX, profSelY, arrowBtnW, arrowBtnH)) {
                        cycleRuleProfile(1);
                        return true;
                    }
                }
            }

            // [ Отмена / Cancel ]
            float btnH = 20.0F;
            float btnY = winY + 172.0F;
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

    private void cycleRuleProfile(int delta) {
        List<ProxyConfig.ProxyProfile> list = config.getProfiles();
        if (list.isEmpty()) return;
        int current = 0;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().equalsIgnoreCase(ruleTargetProfile)) {
                current = i;
                break;
            }
        }
        int next = (current + delta + list.size()) % list.size();
        ruleTargetProfile = list.get(next).getName();
        syncCurrentToRule();
        GuiSounds.picker(true);
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
                case RULE_PATTERN -> {
                    if (rulePatternText.length() < 128 && (Character.isLetterOrDigit(c) || c == '.' || c == '-' || c == '*' || c == '_' || c == ':')) {
                        rulePatternText += c;
                        syncCurrentToRule();
                    }
                }
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
            if (currentTab == ScreenTab.GENERAL) {
                focusedField = switch (focusedField) {
                    case NONE, PASS, RULE_PATTERN -> Field.HOST;
                    case HOST -> Field.PORT;
                    case PORT -> Field.USER;
                    case USER -> Field.PASS;
                };
            } else {
                focusedField = Field.RULE_PATTERN;
            }
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
                        case RULE_PATTERN -> {
                            rulePatternText = (rulePatternText + clip).substring(0, Math.min(128, rulePatternText.length() + clip.length()));
                            syncCurrentToRule();
                        }
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
                    case RULE_PATTERN -> {
                        if (!rulePatternText.isEmpty()) {
                            rulePatternText = rulePatternText.substring(0, rulePatternText.length() - 1);
                            syncCurrentToRule();
                        }
                    }
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
            if (res.success()) {
                ProxyConfig.ProxyProfile p = config.getActiveProfile();
                if (p != null) {
                    p.setLastPingMs(res.pingMs());
                }
            }
        });
    }

    private void saveConfig() {
        syncCurrentToProfile();
        syncCurrentToRule();
        config.setEnabled(enabled);
        config.setHudEnabled(hudEnabled);
        config.setAutoFailover(autoFailover);
        config.setDnsLeakProtection(dnsLeakProtection);
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
