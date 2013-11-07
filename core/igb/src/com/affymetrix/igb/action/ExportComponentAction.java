package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.shared.ExportDialog;
import java.awt.Adjustable;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.logging.Level;

/**
 *
 * @author hiralv Modified by nick
 */
public abstract class ExportComponentAction extends GenericAction {

	private static final long serialVersionUID = 1l;

	protected ExportComponentAction(String text, String iconPath, String largeIconPath) {
		super(text, iconPath, largeIconPath);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		Component component = determineSlicedComponent();
		if (component == null) {
			return;
		}

		try {
			ExportDialog.getSingleton().setComponent(component);
			ExportDialog.getSingleton().initImageInfo();
			ExportDialog.getSingleton().initSeqViewListener(component, getScroller());
			ExportDialog.getSingleton().display(true);
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem during output.", ex, Level.SEVERE);
		}
	}

	public abstract Component determineSlicedComponent();
	
	public abstract Adjustable getScroller();
}
