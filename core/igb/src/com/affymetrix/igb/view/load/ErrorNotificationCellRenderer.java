package com.affymetrix.igb.view.load;

import com.affymetrix.common.CommonUtils;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genoviz.swing.JTextButtonCellRenderer;
import java.awt.event.ActionEvent;
import java.util.logging.Level;
import javax.swing.Icon;
import javax.swing.JButton;

/**
 *
 * @author dcnorris
 */
public class ErrorNotificationCellRenderer extends JTextButtonCellRenderer {
	private static final long serialVersionUID = 1L;
	private final String title;
	private final Icon icon;
	private final String message;
	
	public ErrorNotificationCellRenderer(String title, String message, Icon icon) {
		this.title = title;
		this.message = message;
		this.icon = icon;
	}

	@Override
	protected JButton getButton() {
		return new JButton(icon);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		ErrorHandler.errorPanel(title , message ,Level.WARNING);
	}
}
