
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.util.ExportDialog;
import com.affymetrix.igb.util.ExportDialogGUI;
import java.awt.Component;
import java.awt.event.ActionEvent;

/**
 *
 * @author hiralv
 * Modified by nick
 */
public abstract class ExportComponentAction extends GenericAction {
	private static final long serialVersionUID = 1l;


	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		Component component = determineSlicedComponent();
		if (component == null) {
			return;
		}

		try {
			ExportDialog.setComponent(component);
			ExportDialogGUI.getSingleton().display(true);
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem during output.", ex);
		}
	}

	public abstract Component determineSlicedComponent(); 
		
}

