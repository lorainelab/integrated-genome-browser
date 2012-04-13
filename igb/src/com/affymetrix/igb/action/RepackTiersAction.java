package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.view.SeqMapView;

/**
 * note - this class contains an instance of SeqMapView. For now, there
 * is just one instance using the regular SeqMapView, no instance for
 * AltSpliceView
 */
public abstract class RepackTiersAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;

	protected RepackTiersAction(SeqMapView gviewer, String text, String tooltip, String iconPath) {
		super(gviewer, text, tooltip, iconPath);
	}

	public void repack(final boolean full_repack) {
		AbstractAction action = new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				handler.repackTheTiers(full_repack, true);
			}
		};
		
		gviewer.preserveSelectionAndPerformAction(action);
	}

	/**
	 * Handles tier (track) repacking actions.
	 *
	 * @param theTiers generally either all or selected tiers.
	 */
	protected void repackTiers(List<TierLabelGlyph> theTiers) {
		ViewI ourView = gviewer.getSeqMap().getView();
		for (TierLabelGlyph tl : theTiers) {
			TierGlyph t = (TierGlyph) tl.getInfo();
			int a = t.getSlotsNeeded(ourView);
			ITrackStyleExtended style = t.getAnnotStyle();
			TierGlyph.Direction d = t.getDirection();
			switch (d) {
				case REVERSE:
					style.setReverseMaxDepth(a);
					break;
				default:
				case FORWARD:
					style.setForwardMaxDepth(a);
					break;
			}
			com.affymetrix.igb.shared.ViewModeGlyph vmg = t.getViewModeGlyph();
			if (vmg instanceof com.affymetrix.igb.shared.AbstractGraphGlyph) {
				// So far this has only been tested with annotation depth graphs.
				com.affymetrix.igb.shared.AbstractGraphGlyph gg
						= (com.affymetrix.igb.shared.AbstractGraphGlyph) vmg;
				gg.setVisibleMaxY(a);
			}
		}
		// Now repack with the newly appointed maxima.
		repack(true);
	}
}
