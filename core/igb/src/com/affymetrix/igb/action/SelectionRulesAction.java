package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.IGB;
/**
 *
 * @author auser
 */
public class SelectionRulesAction extends GenericAction{
	private static final long serialVersionUID = 1l;
	private static final SelectionRulesAction ACTION = new SelectionRulesAction();
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static SelectionRulesAction getAction() {
		return ACTION;
	}

	private SelectionRulesAction() {
		super(null, "toolbarButtonGraphics/general/Information16.gif", "toolbarButtonGraphics/general/Information16.gif", null,KeyEvent.VK_A, null, true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		JPanel message_pane = new JPanel();
		message_pane.setLayout(new BoxLayout(message_pane, BoxLayout.Y_AXIS));
		JTextArea rules_text = new JTextArea();
		rules_text.setEditable(false);
		String text = "\t\tSelection Rules in IGB\n\n"
				+ "1. Click on an annotation to select it.\n"
				+ "2. Double-click something to zoom in on it.\n"
				+ "3. Click-drag a region to select and count many items.\n"
				+ "4. Click-SHIFT to add to the currently selected items.\n"
				+ "5. Control-SHIFT click to remove an item from the currently selected items.\n"
				+ "6. Click-drag the axis to zoom in on a region.\n";				
		rules_text.append(text);
		message_pane.add(rules_text);
		final JOptionPane pane = new JOptionPane(message_pane, JOptionPane.INFORMATION_MESSAGE,
				JOptionPane.DEFAULT_OPTION);
		final JDialog dialog = pane.createDialog(IGB.getSingleton().getFrame(), "Selection Rules");
		dialog.setVisible(true);
	}
}
