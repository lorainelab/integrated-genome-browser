package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.util.ComponentWriter;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class ExportWholeFrameAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final ExportWholeFrameAction ACTION = new ExportWholeFrameAction();

	public static ExportWholeFrameAction getAction() {
		return ACTION;
	}

	private ExportWholeFrameAction() {
		super();
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		try {
			ComponentWriter.showExportDialog(IGB.getSingleton().getFrame());
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem during output.", ex);
		}
	}

	@Override
	public String getText() {
		return MessageFormat.format(
				BUNDLE.getString("menuItemHasDialog"),
				BUNDLE.getString("wholeFrame"));
	}
}
