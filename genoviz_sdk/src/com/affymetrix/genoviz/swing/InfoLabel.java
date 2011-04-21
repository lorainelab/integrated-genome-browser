package com.affymetrix.genoviz.swing;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.ToolTipManager;

/**
 *
 * @author lorainelab
 */
public class InfoLabel extends JLabel implements MouseListener{
	private static final int original_dismissDelay = ToolTipManager.sharedInstance().getDismissDelay();
    private static final int original_initialDelay = ToolTipManager.sharedInstance().getInitialDelay();
	
	public InfoLabel(ImageIcon imageIcon){
		super(imageIcon);
		setBounds(0,0,10,10);
		addMouseListener(this);
	}
			
	public void mouseEntered(MouseEvent me) {
		ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
        ToolTipManager.sharedInstance().setInitialDelay(0);
	}

	public void mouseExited(MouseEvent me) {
		ToolTipManager.sharedInstance().setDismissDelay(original_dismissDelay);
        ToolTipManager.sharedInstance().setInitialDelay(original_initialDelay);
	}
	
	public void mouseClicked(MouseEvent me) {}
	public void mousePressed(MouseEvent me) {}
	public void mouseReleased(MouseEvent me) {}

}
