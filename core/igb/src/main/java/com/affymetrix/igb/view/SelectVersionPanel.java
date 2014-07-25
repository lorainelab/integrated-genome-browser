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
    final String first = "Integrated Genome Browser";
    final String second = "Next, choose a genome version using the ";
    final String third = "Current Genome ";
    final String fourth = "tab at right.";

    @Override
    public void paintComponent(Graphics g2) {
        super.paintComponent(g2);
        Graphics2D g = (Graphics2D) g2;
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        Font f = new Font("Sans Serif", Font.PLAIN, 20);
        Font fb = new Font("Sans Serif", Font.BOLD, 20);
        Font f2 = new Font("Sans Serif", Font.PLAIN, 30);
        FontMetrics m = g.getFontMetrics(f);
        FontMetrics m1 = g.getFontMetrics(fb);
        FontMetrics m2 = g.getFontMetrics(f2);

        int yPos = (int) (.50 * this.getHeight()) - 25;

        g.setFont(f2);
        g.setColor(Color.decode("#fffb86"));
        int xPos = (this.getWidth() / 2) - m2.stringWidth(first) / 2;
        yPos += 25;
        g.drawString(first, xPos, yPos);

        g.setFont(f);
        xPos = (this.getWidth() / 2) - m.stringWidth(second + third + fourth) / 2;
        g.setColor(Color.decode("#FFFFFF"));
        yPos += 25;
        g.drawString(second, xPos, yPos);

        g.setFont(fb);
        xPos += m.stringWidth(second);
        g.setColor(Color.decode("#FFFFFF"));
        g.drawString(third, xPos, yPos);

        g.setFont(f);
        xPos += m1.stringWidth(third);
        g.setColor(Color.decode("#FFFFFF"));
        g.drawString(fourth, xPos, yPos);

    }
}
