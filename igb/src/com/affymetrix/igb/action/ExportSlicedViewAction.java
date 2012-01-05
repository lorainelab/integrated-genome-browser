package com.affymetrix.igb.action;

import com.affymetrix.igb.util.ExportDialogGUI;
import java.awt.Component;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.view.AltSpliceView;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class ExportSlicedViewAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final ExportSlicedViewAction ACTION = new ExportSlicedViewAction();

	public static ExportSlicedViewAction getAction() {
		return ACTION;
	}

	private ExportSlicedViewAction() {
		super();
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		Component slice_component = determineSlicedComponent();
		if (slice_component == null) {
			return;
		}

		try {
		//	ComponentWriter.showExportDialog(slice_component);
			ExportDialogGUI.getSingleton().display(slice_component);
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem during output.", ex);
		}
	}

	public Component determineSlicedComponent() {
		AltSpliceView slice_view = (AltSpliceView)((IGB)IGB.getSingleton()).getView(AltSpliceView.class.getName());
		if (slice_view == null) {
			return null;
		}
		return ((AffyLabelledTierMap)slice_view.getSplicedView().getSeqMap()).getSplitPane();
	}

	@Override
	public String getText() {
		return BUNDLE.getString("slicedViewWithLabels");
	}

	@Override
	public int getMnemonic() {
		return KeyEvent.VK_S;
	}

	@Override
	public boolean usePrefixInMenu() {
		return true;
	}

	@Override
	public boolean isPopup() {
		return true;
	}
}
