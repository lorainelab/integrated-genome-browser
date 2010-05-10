package com.affymetrix.igb.action;

import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.util.ComponentWriter;
import com.affymetrix.igb.view.AltSpliceView;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import javax.swing.AbstractAction;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class ExportSlicedViewAction extends AbstractAction {
	private static final long serialVersionUID = 1l;

	public ExportSlicedViewAction() {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("slicedViewWithLabels")));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_S);
	}

	public void actionPerformed(ActionEvent e) {
		AltSpliceView slice_view = null;
		for (Object plugin : ((IGB)IGB.getSingleton()).getPlugins()) {
			if (plugin instanceof AltSpliceView) {
				slice_view = (AltSpliceView) plugin;
				break;
			}
		}

		if (slice_view == null) { return; }

		try {
			if (slice_view != null) {
				AffyLabelledTierMap tm = (AffyLabelledTierMap) slice_view.getSplicedView().getSeqMap();
				ComponentWriter.showExportDialog(tm.getSplitPane());
			}
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem during output.", ex);
		}
	}
}
