package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.util.ErrorHandler;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.shared.ExportDialog;
import java.util.logging.Level;

/**
 *
 * @author nick
 */
public class ExportImageAction extends GenericAction {

	private static final long serialVersionUID = 1l;
	private static final ExportImageAction ACTION = new ExportImageAction();

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ExportImageAction getAction() {
		return ACTION;
	}

	private ExportImageAction() {
		super(BUNDLE.getString("exportImage"), BUNDLE.getString("exportImageTooltip"),
				"16x16/actions/export_image.png",
				"22x22/actions/export_image.png",
				KeyEvent.VK_UNDEFINED, null, true);
		this.ordinal = -9002000;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);

		try {
			ExportDialog.getSingleton().display(false);
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem during output.", ex, Level.SEVERE);
		}
	}

}
