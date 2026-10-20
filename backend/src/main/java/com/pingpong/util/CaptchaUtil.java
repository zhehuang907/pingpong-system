package com.pingpong.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * 验证码工具类
 */
public class CaptchaUtil {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LENGTH = 4;
    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * 生成验证码图片
     *
     * @return BufferedImage 验证码图片
     */
    public static BufferedImage generateCaptchaImage(String captchaCode) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 设置背景色
        g.setColor(getRandomColor(200, 250));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 绘制干扰线
        drawRandomLines(g, 20);

        // 绘制验证码字符
        drawCaptchaString(g, captchaCode);

        // 绘制干扰点
        drawRandomPoints(g, 200);

        g.dispose();
        return image;
    }

    /**
     * 生成随机验证码
     *
     * @return 验证码字符串
     */
    public static String generateCaptchaCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    /**
     * 生成随机颜色
     */
    private static Color getRandomColor(int min, int max) {
        Random random = new Random();
        int r = min + random.nextInt(max - min);
        int g = min + random.nextInt(max - min);
        int b = min + random.nextInt(max - min);
        return new Color(r, g, b);
    }

    /**
     * 绘制随机线条
     */
    private static void drawRandomLines(Graphics2D g, int count) {
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            g.setColor(getRandomColor(100, 200));
            int x1 = random.nextInt(WIDTH);
            int y1 = random.nextInt(HEIGHT);
            int x2 = random.nextInt(WIDTH);
            int y2 = random.nextInt(HEIGHT);
            g.setStroke(new BasicStroke(1.5f));
            g.drawLine(x1, y1, x2, y2);
        }
    }

    /**
     * 绘制验证码字符串
     */
    private static void drawCaptchaString(Graphics2D g, String captchaCode) {
        Random random = new Random();
        int fontSize = HEIGHT - 8;
        int charWidth = WIDTH / CODE_LENGTH;

        for (int i = 0; i < captchaCode.length(); i++) {
            // 随机字体
            String[] fonts = {"Arial", "Verdana", "Tahoma", "Times New Roman"};
            g.setFont(new Font(fonts[random.nextInt(fonts.length)], Font.BOLD, fontSize));

            // 随机颜色和旋转
            g.setColor(getRandomColor(50, 150));
            Graphics2D g2d = (Graphics2D) g.create();

            // 计算字符位置
            int x = i * charWidth + 10;
            int y = HEIGHT / 2 + fontSize / 2 - 4;

            // 随机旋转角度（-15 到 15 度）
            double rotation = (Math.random() - 0.5) * 0.3;
            g2d.rotate(rotation, x, y);

            // 绘制字符
            g2d.drawString(String.valueOf(captchaCode.charAt(i)), x, y);
            g2d.dispose();
        }
    }

    /**
     * 绘制随机点
     */
    private static void drawRandomPoints(Graphics2D g, int count) {
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            g.setColor(getRandomColor(100, 200));
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            g.fillOval(x, y, 2, 2);
        }
    }
}
