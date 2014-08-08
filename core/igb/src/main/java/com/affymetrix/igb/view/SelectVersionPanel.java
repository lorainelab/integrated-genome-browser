/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 *
 * @author tkanapar
 */
public class SelectVersionPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    public SelectVersionPanel() {
        this.setBackground(Color.black);
    }
    private final String firstLine = "Welcome to Integrated Genome Browser";
    private final String secondLine = "You selected a species. Next, select a genome version.";
    private final String inlineArrowText = "Select genome version.";

    @Override
    public void paintComponent(Graphics g2) {
        super.paintComponent(g2);
        Graphics2D g = (Graphics2D) g2;
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        Font smallFont = new Font("Sans Serif", Font.PLAIN, 20);
        Font largeFont = new Font("Sans Serif", Font.PLAIN, 30);
        FontMetrics m = g.getFontMetrics(smallFont);
        FontMetrics m2 = g.getFontMetrics(largeFont);

        int fixedArrowYpos = 50;
        int[] xPoints = {this.getWidth() - 10, this.getWidth() - 10, this.getWidth()};
        int[] yPoints = {fixedArrowYpos - 10, fixedArrowYpos + 10, fixedArrowYpos};
        int yPos = (int) (.50 * this.getHeight()) - 25;

        g.setColor(Color.decode("#fffb86"));
        g.drawLine(this.getWidth() - 40, fixedArrowYpos, this.getWidth() - 10, fixedArrowYpos);
        g.fillPolygon(xPoints, yPoints, xPoints.length);

        g.setFont(smallFont);
        int xPos = this.getWidth() - (m.stringWidth(inlineArrowText) + 40);
        g.drawString(inlineArrowText, xPos, fixedArrowYpos + 5);

        g.setFont(largeFont);
        g.setColor(Color.decode("#fffb86"));
        xPos = (this.getWidth() / 2) - m2.stringWidth(firstLine) / 2;
        yPos += 25;
        g.drawString(firstLine, xPos, yPos);

        g.setFont(smallFont);
        xPos = (this.getWidth() / 2) - m.stringWidth(secondLine) / 2;
        g.setColor(Color.decode("#FFFFFF"));
        yPos += 25;
        g.drawString(secondLine, xPos, yPos);

    }
}
