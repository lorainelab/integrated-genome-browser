package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.util.ComponentWriter;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class ExportLabelledMainViewAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final ExportLabelledMainViewAction ACTION = new ExportLabelledMainViewAction();

	public static ExportLabelledMainViewAction getAction() {
		return ACTION;
	}

	private ExportLabelledMainViewAction() {
		super();
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		try {
			AffyLabelledTierMap tm = (AffyLabelledTierMap) IGB.getSingleton().getMapView().getSeqMap();
			ComponentWriter.showExportDialog(tm.getSplitPane());
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem during output.", ex);
		}
	}

	@Override
	public String getText() {
		return MessageFormat.format(
				BUNDLE.getString("menuItemHasDialog"),
				BUNDLE.getString("mainViewWithLabels"));
	}

	@Override
	public int getShortcut() {
		return KeyEvent.VK_L;
	}

	@Override
	public boolean usePrefixInMenu() {
		return true;
	}
}
