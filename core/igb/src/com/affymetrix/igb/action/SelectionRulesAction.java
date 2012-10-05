package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.JTextArea;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.igb.IGB;
/**
 *
 * @author auser
 */
public class SelectionRulesAction extends GenericAction{
	private static final long serialVersionUID = 1l;

	public SelectionRulesAction() {
		super(null, "toolbarButtonGraphics/general/Information16.gif", "toolbarButtonGraphics/general/Information16.gif", null,KeyEvent.VK_A, null, true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JFrame messageFrame = new JFrame("Selection Rules in IGB");
		JTextArea rules_text = new JTextArea();
		rules_text.setEditable(false);
		String text = 
				  "1. Click on an annotation to select it.\n"
				+ "2. Double-click something to zoom in on it.\n"
				+ "3. Click-drag a region to select and count many items.\n"
				+ "4. Click-SHIFT to add to the currently selected items.\n"
				+ "5. Control-SHIFT click to remove an item from the currently selected items.\n"
				+ "6. Click-drag the axis to zoom in on a region.\n";				
		rules_text.append(text);
		messageFrame.add(rules_text);
		messageFrame.pack();
		messageFrame.setLocationRelativeTo(IGB.getSingleton().getFrame());
		messageFrame.setVisible(true);
	}
}
