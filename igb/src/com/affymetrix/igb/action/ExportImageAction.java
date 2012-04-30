package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.util.ExportDialogGUI;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import com.affymetrix.genometryImpl.event.GenericAction;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author nick
 */
public class ExportImageAction extends GenericAction {

	private static final long serialVersionUID = 1l;
	private static final ExportImageAction ACTION = new ExportImageAction();

	public static ExportImageAction getAction() {
		return ACTION;
	}

	private ExportImageAction() {
		super(BUNDLE.getString("exportImage"), null, "16x16/mimetypes/image-x-generic.png", "22x22/mimetypes/image-x-generic.png", KeyEvent.VK_UNDEFINED, null, true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);

		try {
			ExportDialogGUI.getSingleton().display(false);
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem during output.", ex);
		}
	}
}
