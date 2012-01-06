/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view.welcome;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * This class manages the JPanel component using Graphics2D to better handle the 
 * look of the header on IGB's Welcome page.  The title and subtitle are 
 * kept centered on the main screen (top left screen) for different windown sizes.
 * @author jfvillal
 */
public class WelcomeTitle extends JPanel {

	public WelcomeTitle(){
		this.setBackground(Color.black);
                
	}
	final String first = "Welcome to";
	final String second = "Integrated Genome Browser";
	final String third = "To get started, choose a ";
	final String fourth = "species ";
	final String fifth = "and ";
	final String sixth = "genome version.";
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

		int start = (int)( .50 * this.getHeight() ) - 43;
		//g.drawString( this.getWidth() +","+this.getHeight(), 20 , 20);

		g.setFont(f);
		g.setColor(Color.decode("#FFFFFF"));//Color.decode("#fffb86") );
		int width = (this.getWidth() / 2 ) - m.stringWidth( first )/2;
		//+ " "+ this.getWidth() + "," + this.getHeight()
		g.drawString(first, width , start);


		g.setFont(f2);
		g.setColor(Color.decode("#fffb86"));
		width = (this.getWidth() / 2 ) - m2.stringWidth( second )/2;
		start += 48;
		g.drawString(second , width , start);


		int line3 = start + 48;
		g.setFont(f);
		width = (this.getWidth() / 2 ) - m.stringWidth( third + fourth + fifth + sixth )/2;
		g.setColor(Color.decode("#FFFFFF"));
		g.drawString(third, width , line3 );


		width += m.stringWidth(third);
		g.setColor(Color.decode("#fffb86"));
		g.drawString(fourth, width , line3);

		width += m.stringWidth(fourth);
		g.setColor(Color.decode("#FFFFFF"));
		g.drawString(fifth, width , line3);

		width += m.stringWidth(fifth);
		g.setColor(Color.decode("#fffb86"));
g.drawString(sixth, width , line3);
	}
	
}