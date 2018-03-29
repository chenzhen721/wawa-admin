package com.wawa.common.util;


import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * date: 13-8-23 下午3:57
 *
 * @author: yangyang.cong@ttpod.com
 */
public abstract class Captcha {

    static final double SKEW = 0.5;
    static final int DRAW_LINES =3;
    static final int DRAW_BOXES = 300;
    private static final Color[] RANDOM_BG_COLORS = {
            Color.RED, Color.CYAN, Color.GREEN, Color.MAGENTA, Color.ORANGE, Color.PINK,
            Color.YELLOW};

    private static final Color [] RANDOM_FG_COLORS = {
            Color.BLACK, Color.BLUE, Color.DARK_GRAY};

    public static void draw(String securityChars,int MAX_X,int MAX_Y,OutputStream out ) {
        BufferedImage outImage = new BufferedImage(MAX_X, MAX_Y,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = outImage.createGraphics();
        g2d.setColor(java.awt.Color.WHITE);
        g2d.fillRect(0, 0, MAX_X, MAX_Y);
        for (int i = 0; i < DRAW_BOXES; i++) { // paindBoxes
            int colorId = (int) (Math.random() * RANDOM_BG_COLORS.length);
            g2d.setColor(RANDOM_BG_COLORS[colorId]);
            g2d.fillRect((int) (Math.random() * MAX_X), (int) (Math.random() * MAX_Y),
                    (int) (Math.random() * MAX_X), (int) (Math.random() * MAX_Y));
        }

        Font font = new Font("dialog", Font.BOLD, 33);
        g2d.setFont(font);

        g2d.setColor(Color.BLACK);
        g2d.drawRect(0, 0, (MAX_X) - 1, MAX_Y - 1);

        AffineTransform affineTransform = new AffineTransform();
        int MAX_LETTER_COUNT = securityChars.length();

        int LETTER_WIDTH = MAX_X/MAX_LETTER_COUNT;
        for (int i = 0; i < MAX_LETTER_COUNT; i++) {
            double angle = 0;
            if (Math.random() * 2 > 1) {
                angle = Math.random() * SKEW;
            } else {
                angle = Math.random() * -SKEW;
            }
            affineTransform.rotate(angle, (LETTER_WIDTH * i) + (LETTER_WIDTH / 2), MAX_Y / 2);
            // g2d.setTransform(affineTransform);
            // setRandomFont(g2d);
            // setRandomFGColor(g2d);
            g2d.drawString(securityChars.substring(i, i + 1),
                    (i * LETTER_WIDTH) + 3, 28 + (int) (Math.random() * 6));

            affineTransform.rotate(-angle, (LETTER_WIDTH * i) + (LETTER_WIDTH / 2), MAX_Y / 2);
        }

        /*g2d.setXORMode(Color.RED);
        g2d.setStroke(new BasicStroke(10));
        g2d.drawLine(0,0,MAX_X, MAX_Y);
        g2d.setXORMode(Color.YELLOW);
        g2d.drawLine(0,MAX_Y,MAX_X, 0);*/

        g2d.setXORMode(RANDOM_BG_COLORS[(int) (Math.random() * RANDOM_BG_COLORS.length)]);
        g2d.setStroke(new BasicStroke(10));
        g2d.drawLine(0, MAX_Y / 2, MAX_X, MAX_Y / 2);
        g2d.setXORMode(RANDOM_BG_COLORS[(int) (Math.random() * RANDOM_BG_COLORS.length)]);
        g2d.drawLine(0, (MAX_Y / 2) - 10, MAX_X, (MAX_Y / 2) - 10);
        g2d.setXORMode(RANDOM_BG_COLORS[(int) (Math.random() * RANDOM_BG_COLORS.length)]);
        g2d.drawLine(0, (MAX_Y / 2) + 10, MAX_X, (MAX_Y / 2) + 10);

        /*for (int i = 0; i < DRAW_LINES; i ++) {
            g2d.setXORMode(Color.RED);
            // setRandomBGColor(g2d);
            g2d.setStroke(new BasicStroke(4));
            //affineTransform.rotate(0);
            int y1 = (int) (Math.random() * MAX_Y);
            //int y2 = (int) (Math.random() * MAX_Y);
             g2d.drawLine(0, y1,
                    MAX_X, y1);

        }*/
        OutputStream buff = new BufferedOutputStream(out);
        try {
            ImageIO.write(outImage, "png", buff);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            IOUtils.closeQuietly(buff);
        }
    }

}
