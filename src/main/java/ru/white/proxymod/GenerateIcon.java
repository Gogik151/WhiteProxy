package ru.white.proxymod;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class GenerateIcon {
    public static void main(String[] args) throws Exception {
        int size = 512;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // 1. Фон с закругленными углами
        int arc = 110;
        RoundRectangle2D bg = new RoundRectangle2D.Float(16, 16, 480, 480, arc, arc);

        Paint bgGradient = new GradientPaint(16, 16, new Color(15, 23, 42), 496, 496, new Color(6, 9, 17));
        g.setPaint(bgGradient);
        g.fill(bg);

        // Внутреннее мягкое свечение
        g.setPaint(new Color(59, 130, 246, 25));
        g.setStroke(new BasicStroke(16.0f));
        g.draw(new RoundRectangle2D.Float(24, 24, 464, 464, arc - 10, arc - 10));

        // Основная неоновая рамка
        Paint borderGradient = new GradientPaint(16, 16, new Color(59, 130, 246), 496, 496, new Color(6, 182, 212));
        g.setPaint(borderGradient);
        g.setStroke(new BasicStroke(5.0f));
        g.draw(bg);

        // 2. Декоративная кибер-сетка (тонкие линии)
        g.setColor(new Color(59, 130, 246, 20));
        g.setStroke(new BasicStroke(1.5f));
        for (int i = 80; i < 440; i += 40) {
            g.drawLine(i, 40, i, 472);
            g.drawLine(40, i, 472, i);
        }

        // 3. Центральный символ: Защитный щит сети (Proxy Shield)
        float cx = 256;
        float cy = 240;

        // Неоновый ореол за щитом
        g.setPaint(new RadialGradientPaint(cx, cy, 140, new float[]{0.0f, 0.7f, 1.0f},
                new Color[]{new Color(59, 130, 246, 50), new Color(6, 182, 212, 20), new Color(0, 0, 0, 0)}));
        g.fill(new Ellipse2D.Float(cx - 140, cy - 140, 280, 280));

        // Контур щита
        Path2D.Float shield = new Path2D.Float();
        shield.moveTo(cx, 130);
        shield.curveTo(cx + 60, 130, cx + 105, 145, cx + 105, 185);
        shield.curveTo(cx + 105, 275, cx + 60, 325, cx, 355);
        shield.curveTo(cx - 60, 325, cx - 105, 275, cx - 105, 185);
        shield.curveTo(cx - 105, 145, cx - 60, 130, cx, 130);
        shield.closePath();

        // Заливка щита полупрозрачной стеклянной подложкой
        g.setPaint(new GradientPaint(cx, 130, new Color(20, 35, 65, 220), cx, 355, new Color(10, 18, 35, 240)));
        g.fill(shield);

        // Обводка щита
        g.setPaint(new GradientPaint(cx - 100, 130, new Color(96, 165, 250), cx + 100, 355, new Color(45, 212, 191)));
        g.setStroke(new BasicStroke(6.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(shield);

        // Внутренний акцент щита
        g.setColor(new Color(59, 130, 246, 80));
        g.setStroke(new BasicStroke(2.0f));
        Path2D.Float innerShield = new Path2D.Float();
        innerShield.moveTo(cx, 148);
        innerShield.curveTo(cx + 50, 148, cx + 88, 160, cx + 88, 192);
        innerShield.curveTo(cx + 88, 265, cx + 50, 308, cx, 335);
        innerShield.curveTo(cx - 50, 308, cx - 88, 265, cx - 88, 192);
        innerShield.curveTo(cx - 88, 160, cx - 50, 148, cx, 148);
        innerShield.closePath();
        g.draw(innerShield);

        // 4. Сетевые узлы и перекрестия внутри щита (Proxy Routing)
        g.setColor(new Color(255, 255, 255, 220));
        g.fill(new Ellipse2D.Float(cx - 6, cy - 35, 12, 12));
        g.fill(new Ellipse2D.Float(cx - 45, cy + 25, 10, 10));
        g.fill(new Ellipse2D.Float(cx + 45, cy + 25, 10, 10));
        g.fill(new Ellipse2D.Float(cx - 6, cy + 55, 12, 12));

        g.setColor(new Color(96, 165, 250));
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine((int)cx, (int)cy - 29, (int)cx - 40, (int)cy + 25);
        g.drawLine((int)cx, (int)cy - 29, (int)cx + 40, (int)cy + 25);
        g.drawLine((int)cx - 40, (int)cy + 25, (int)cx, (int)cy + 55);
        g.drawLine((int)cx + 40, (int)cy + 25, (int)cx, (int)cy + 55);

        // Центральный замок / ядро
        g.setColor(new Color(255, 255, 255));
        g.fill(new RoundRectangle2D.Float(cx - 14, cy - 2, 28, 22, 6, 6));
        g.setStroke(new BasicStroke(3.5f));
        g.draw(new Arc2D.Float(cx - 10, cy - 14, 20, 20, 0, 180, Arc2D.OPEN));

        // 5. Текст "WHITE PROXY" внизу
        g.setColor(new Color(245, 248, 255));
        g.setFont(new Font("Segoe UI", Font.BOLD, 30));
        FontMetrics fm = g.getFontMetrics();
        String brand = "WHITE PROXY";
        int textW = fm.stringWidth(brand);
        g.drawString(brand, (int)(cx - textW / 2.0), 425);

        g.dispose();

        // Сохраняем на Рабочий стол
        String userHome = System.getProperty("user.home");
        File desktop = new File(userHome, "Desktop");
        File outFile = new File(desktop, "whiteproxy_icon.png");
        ImageIO.write(img, "PNG", outFile);
        System.out.println("Icon saved to: " + outFile.getAbsolutePath());
    }
}
