package com.affymetrix.igb.action;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JOptionPane;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;

public class RenameAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static final RenameAction ACTION = new RenameAction();

	public static RenameAction getAction() {
		return ACTION;
	}

	private RenameAction() {
		super(BUNDLE.getString("renameAction"), null, null);
	}

	private void renameTier(final TierGlyph tier) {
		if (tier == null) {
			return;
		}
		ITrackStyleExtended style = tier.getAnnotStyle();

		String new_label = JOptionPane.showInputDialog(BUNDLE.getString("label") + ": ", style.getTrackName());
		if (new_label != null && new_label.length() > 0) {
			style.setTrackName(new_label);
			tier.setLabel(new_label);
			getSeqMapView().getSeqMap().setTierLabels();
		}
		getSeqMapView().getSeqMap().updateWidget();
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		List<TierGlyph> current_tiers = getTierManager().getSelectedTiers();
		if (current_tiers.size() != 1) {
			ErrorHandler.errorPanel(BUNDLE.getString("multTrackError"));
		}
		TierGlyph current_tier = current_tiers.get(0);
		renameTier(current_tier);
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}
}
