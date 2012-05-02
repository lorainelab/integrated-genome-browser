package com.affymetrix.igb.shared;

import com.affymetrix.genoviz.swing.recordplayback.JRPButton;
import com.affymetrix.igb.action.ChangeColorActionA;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author hiralv
 */
public class JRPColoredButton extends JRPButton implements ListSelectionListener{
	private static final Font background_font = new Font(Font.MONOSPACED, Font.BOLD, 34);
	private static final Font foreground_font = new Font(Font.MONOSPACED, Font.BOLD, 28);
	
	public JRPColoredButton(String id, ChangeColorActionA a) {
		super(id, a);
	}

	public void valueChanged(ListSelectionEvent e) {
		setBackground(((ChangeColorActionA)getAction()).getBackgroundColor());
		setForeground(((ChangeColorActionA)getAction()).getForegroundColor());
		repaint();
	}

	@Override
	public void paint(Graphics g){
		super.paint(g);
		
		// Draw Background
		g.setColor(getBackground());
		g.fillRect(2, 2, getSize().width-4, getSize().height-4);
		
		// Draw Foreground
		g.setFont(background_font);
		g.setColor(Color.BLACK);
		g.drawString("A", 6, 22);
		
		// Draw Foreground
		g.setFont(foreground_font);
		g.setColor(getForeground());
		g.drawString("A", 8, 20);
	}
}
