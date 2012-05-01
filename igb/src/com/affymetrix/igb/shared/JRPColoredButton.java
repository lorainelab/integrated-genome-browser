package com.affymetrix.igb.shared;

import com.affymetrix.genoviz.swing.recordplayback.JRPButton;
import com.affymetrix.igb.action.ChangeColorActionA;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author hiralv
 */
public class JRPColoredButton extends JRPButton implements ListSelectionListener{
	
	public JRPColoredButton(String id, ChangeColorActionA a) {
		super(id, a);
	}

	public void valueChanged(ListSelectionEvent e) {
		this.setBackground(((ChangeColorActionA) this.getAction()).getColor());
		this.repaint();
	}

}
