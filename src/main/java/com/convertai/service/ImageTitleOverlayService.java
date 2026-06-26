package com.convertai.service;

import com.convertai.config.AppConfig;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ImageTitleOverlayService {
    private final AppConfig config;

    public ImageTitleOverlayService(AppConfig config) {
        this.config = config;
    }

    public void drawTitle(Path imagePath, String title) throws IOException {
        if (!config.imageTitleEnabled() || title == null || title.isBlank()) {
            return;
        }

        BufferedImage source = ImageIO.read(imagePath.toFile());
        if (source == null) {
            throw new IOException("无法读取生成后的封面图片: " + imagePath);
        }

        BufferedImage canvas = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int fontSize = Math.max(42, (int) Math.round(source.getHeight() * config.imageTitleFontSizeRatio()));
            int maxWidth = Math.max(200, (int) Math.round(source.getWidth() * config.imageTitleMaxWidthRatio()));
            Font font = loadTitleFont(fontSize);
            graphics.setFont(font);

            List<String> lines = wrapTitle(title, graphics.getFontMetrics(), maxWidth);
            int lineHeight = (int) Math.round(graphics.getFontMetrics().getHeight() * 1.08);
            int totalHeight = lineHeight * lines.size();
            int startY = titleStartY(source.getHeight(), totalHeight);

            for (int i = 0; i < lines.size(); i++) {
                drawCenteredOutlinedText(graphics, lines.get(i), source.getWidth() / 2, startY + i * lineHeight, font);
            }
        } finally {
            graphics.dispose();
        }

        ImageIO.write(canvas, "png", imagePath.toFile());
    }

    private int titleStartY(int imageHeight, int totalHeight) {
        int top = config.imageTitleMarginTop();
        return switch (config.imageTitlePosition().toLowerCase()) {
            case "bottom" -> imageHeight - top - totalHeight;
            case "center" -> (imageHeight - totalHeight) / 2;
            default -> top;
        };
    }

    private void drawCenteredOutlinedText(Graphics2D graphics, String text, int centerX, int baselineY, Font font) {
        FontMetrics metrics = graphics.getFontMetrics(font);
        int x = centerX - metrics.stringWidth(text) / 2;
        int y = baselineY + metrics.getAscent();

        GlyphVector glyphVector = font.createGlyphVector(graphics.getFontRenderContext(), text);
        var shape = glyphVector.getOutline(x, y);

        graphics.setColor(new Color(0, 0, 0, 115));
        graphics.translate(5, 7);
        graphics.fill(shape);
        graphics.translate(-5, -7);

        graphics.setStroke(new BasicStroke(Math.max(4f, font.getSize2D() / 14f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(parseColor(config.imageTitleOutlineColor(), new Color(59, 22, 8)));
        graphics.draw(shape);

        graphics.setColor(parseColor(config.imageTitleFontColor(), new Color(248, 230, 176)));
        graphics.fill(shape);
    }

    private List<String> wrapTitle(String title, FontMetrics metrics, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < title.length(); i++) {
            char c = title.charAt(i);
            String next = current.toString() + c;
            if (!current.isEmpty() && metrics.stringWidth(next) > maxWidth) {
                lines.add(current.toString());
                current.setLength(0);
            }
            current.append(c);
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private Font loadTitleFont(int fontSize) {
        for (Path fontPath : candidateFonts()) {
            if (Files.exists(fontPath)) {
                try {
                    return Font.createFont(Font.TRUETYPE_FONT, fontPath.toFile()).deriveFont(Font.BOLD, (float) fontSize);
                } catch (Exception ignored) {
                    // Continue with the next candidate.
                }
            }
        }
        return new Font("Serif", Font.BOLD | Font.ITALIC, fontSize);
    }

    private List<Path> candidateFonts() {
        List<Path> fonts = new ArrayList<>();
        if (!config.imageTitleFontFile().isBlank()) {
            fonts.add(Path.of(config.imageTitleFontFile()));
        }
        fonts.add(Path.of("C:/Windows/Fonts/STXINGKA.TTF"));
        fonts.add(Path.of("C:/Windows/Fonts/SIMKAI.TTF"));
        fonts.add(Path.of("C:/Windows/Fonts/msyh.ttc"));
        return fonts;
    }

    private Color parseColor(String value, Color fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Color.decode(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
