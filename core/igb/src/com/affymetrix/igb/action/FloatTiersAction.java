package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.ViewModeGlyph;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.util.List;
import javax.swing.SwingUtilities;

public class FloatTiersAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;

	private static final FloatTiersAction ACTION = new FloatTiersAction();

	private Enabler enabler = new Enabler();

	private class Enabler implements SymSelectionListener {

		@Override
		public void symSelectionChanged(SymSelectionEvent evt) {
			// Only pay attention to selections from the main SeqMapView or its map.
			// Ignore the splice view as well as events coming from this class itself.
			Object src = evt.getSource();
			SeqMapView gviewer = getSeqMapView();
			if (!(src == gviewer || src == gviewer.getSeqMap())) {
				return;
			}

			List<SeqSymmetry> selected_syms = SeqMapView.glyphsToSyms(getTierManager().getSelectedTiers());

			boolean hasFloater = false;
			boolean hasAnchored = false;
			for (TierGlyph tg : getSeqMapView().getTierManager().getVisibleTierGlyphs()) {
				ViewModeGlyph vg = tg.getViewModeGlyph();
				if (vg instanceof AbstractGraphGlyph) {
					SeqSymmetry ss = (SeqSymmetry) vg.getInfo();
					if (selected_syms.contains(ss)) { // Need this? Action doesn't.
						boolean floating = vg.getAnnotStyle().getFloatTier();
						hasFloater |= floating;
						hasAnchored |= !floating;
					}
				}
			}
			ACTION.setEnabled(hasAnchored);

		}
		
	};

	static {
		GenericActionHolder.getInstance().addGenericAction(ACTION);
		GenometryModel.getGenometryModel().addSymSelectionListener(ACTION.enabler);
		ACTION.setEnabled(false);
	}
	
	public static FloatTiersAction getAction() {
		return ACTION;
	}

	public FloatTiersAction() {
		super("Float Track", "16x16/actions/float.png", "22x22/actions/float.png");
		this.ordinal = -6006511;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		boolean something_changed = false;
		List<? extends GlyphI> selectedTiers = getTierManager().getSelectedTiers();
		for (GlyphI tg : selectedTiers) {
			ViewModeGlyph gl = ((TierGlyph)tg).getViewModeGlyph();
			if (gl instanceof AbstractGraphGlyph) { // for now, eventually all tracks should float
				ITrackStyleExtended style = gl.getAnnotStyle();
				boolean is_floating = style.getFloatTier();
				if (!is_floating) {
					// figure out correct height
					Rectangle2D.Double coordbox = gl.getCoordBox();
					Rectangle pixbox = new Rectangle();
					getSeqMapView().getSeqMap().getView().transformToPixels(coordbox, pixbox);
					style.setY(pixbox.y);
					style.setHeight(pixbox.height);
	
					style.setFloatTier(true);

					if(gl instanceof AbstractGraphGlyph){
						((AbstractGraphGlyph)gl).getGraphGlyph().setShowLabel(true);
					}

					something_changed = true;
				}
			}
		}
		if (something_changed) {
			updateViewer();
		}
	}

	private void updateViewer() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				SeqMapView v = getSeqMapView();
				GenometryModel m = GenometryModel.getGenometryModel();
				BioSeq s = m.getSelectedSeq();
				v.setAnnotatedSeq(s, true, true);
				v.postSelections(); // to disable partner.
			}
		});
	}
}
