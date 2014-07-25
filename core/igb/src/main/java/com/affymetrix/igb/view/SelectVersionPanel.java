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
    final String first = "Welcome to";
    final String second = "Integrated Genome Browser";
    final String third = "To view species, choose a genome version using the Current Genome tab.";

    @Override
    public void paintComponent(Graphics g2) {
        super.paintComponent(g2);
        Graphics2D g = (Graphics2D) g2;
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        Font f = new Font("Sans Serif", Font.PLAIN, 20);
        Font f2 = new Font("Sans Serif", Font.PLAIN, 30);
        FontMetrics m = g.getFontMetrics(f);
        FontMetrics m2 = g.getFontMetrics(f2);

        int start = (int) (.50 * this.getHeight()) - 25;
        //g.drawString( this.getWidth() +","+this.getHeight(), 20 , 20);

        g.setFont(f);
        g.setColor(Color.decode("#FFFFFF"));//Color.decode("#fffb86") );
        int width = (this.getWidth() / 2) - m.stringWidth(first) / 2;
        //+ " "+ this.getWidth() + "," + this.getHeight()
        g.drawString(first, width, start);

        g.setFont(f2);
        g.setColor(Color.decode("#fffb86"));
        width = (this.getWidth() / 2) - m2.stringWidth(second) / 2;
        start += 35;
        g.drawString(second, width, start);

        g.setFont(f);
        width = (this.getWidth() / 2) - m.stringWidth(third) / 2;
        g.setColor(Color.decode("#FFFFFF"));
        start += 35;
        g.drawString(third, width, start);

    }
}
