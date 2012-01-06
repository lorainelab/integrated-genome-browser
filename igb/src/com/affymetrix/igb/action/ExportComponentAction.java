
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.util.ExportDialogGUI;
import java.awt.Component;
import java.awt.event.ActionEvent;

/**
 *
 * @author hiralv
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
		//	ComponentWriter.showExportDialog(slice_component);
			ExportDialogGUI.getSingleton().display(component);
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem during output.", ex);
		}
	}

	public abstract Component determineSlicedComponent(); 
		
}

