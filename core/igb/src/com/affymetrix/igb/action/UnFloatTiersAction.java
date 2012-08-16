package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.PixelFloaterGlyph;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.ViewModeGlyph;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.tiers.TierLabelManager;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;

public class UnFloatTiersAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;

	private static final UnFloatTiersAction ACTION = new UnFloatTiersAction();

	private UnFloatTiersAction.Enabler enabler = new UnFloatTiersAction.Enabler();

	private class Enabler implements SymSelectionListener {

		/**
		 * React to selection changing
		 * by finding out if any floating tiers are selected.
		 * This isn't doing it right yet.
		 * Need to listen to some other selection?
		 * @param evt 
		 */
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
			List<ViewModeGlyph> selectedTiers = getSelectedFloatingTiers();
			for (ViewModeGlyph vg : selectedTiers) {
				if (vg instanceof AbstractGraphGlyph) {
					//SeqSymmetry ss = (SeqSymmetry) vg.getInfo();
					//if (selected_syms.contains(ss)) { // Need this? Action doesn't.
						boolean floating = vg.getAnnotStyle().getFloatTier();
						hasFloater |= floating;
						hasAnchored |= !floating;
					//}
				}
			}
			ACTION.setEnabled(hasFloater);

		}
		
	};


	static {
		GenericActionHolder.getInstance().addGenericAction(ACTION);
		GenometryModel.getGenometryModel().addSymSelectionListener(ACTION.enabler);
		ACTION.setEnabled(false);
	}
	
	public static UnFloatTiersAction getAction() {
		return ACTION;
	}

	public UnFloatTiersAction() {
		super("Anchor Floating Track",
				"16x16/actions/anchor.png",
				"22x22/actions/anchor.png");
		this.ordinal = -6006512;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		boolean something_changed = false;
		List<ViewModeGlyph> selectedTiers = getSelectedFloatingTiers();
		for (ViewModeGlyph gl : selectedTiers) {
			ITrackStyleExtended style = gl.getAnnotStyle();
			boolean is_floating = style.getFloatTier();
			if (is_floating) {
				//GraphGlyphUtils.attachGraph(gl, gviewer);
				// figure out correct height
//				Rectangle2D.Double tempbox = gl.getCoordBox();  // pixels, since in PixelFloaterGlyph 1:1 mapping of pixel:coord
//				Rectangle pixbox = new Rectangle((int) tempbox.x, (int) tempbox.y, (int) tempbox.width, (int) tempbox.height);
//				Rectangle2D.Double coordbox = new Rectangle2D.Double();
//				getSeqMapView().getSeqMap().getView().transformToCoords(pixbox, coordbox);
				
				Rectangle2D.Double coordbox = gl.getCoordBox();
				style.setY(coordbox.y);
				style.setHeight(coordbox.height);
				style.setFloatTier(false);
				
				if(gl instanceof AbstractGraphGlyph){
					((AbstractGraphGlyph)gl).getGraphGlyph().getGraphState().setShowLabel(false);
				}

				something_changed = true;
			}
		}
		if (something_changed) {
			updateViewer();
		}
	}

	private List<ViewModeGlyph> getSelectedFloatingTiers() {
		List<ViewModeGlyph> selectedTiers = new ArrayList<ViewModeGlyph>();
		SeqMapView v = this.getSeqMapView();
		if (null == v) {
			return selectedTiers;
		}
		PixelFloaterGlyph g = v.getPixelFloater();
		if (null == g) {
			return selectedTiers;
		}
		List<GlyphI> l = g.getChildren();
		if (null == l) {
			return selectedTiers;
		}
		for (GlyphI glyph : l) {
			if (glyph.isSelected()) {
				selectedTiers.add((ViewModeGlyph)glyph);
			}
		}
		return selectedTiers;
	}
	private void updateViewer() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				SeqMapView v = getSeqMapView();
				GenometryModel m = GenometryModel.getGenometryModel();
				BioSeq s = m.getSelectedSeq();
				v.setAnnotatedSeq(s, true, true);
				// Compensating for what I think is a bug in setAnnotatedSeq:
				// Select the label glyph to generate a selection change event.
				TierLabelManager mgr = v.getTierManager();
				List<TierLabelGlyph> labels = mgr.getAllTierLabels();
				for (TierLabelGlyph g : labels) {
					TierGlyph tg = g.getReferenceTier();
					ViewModeGlyph vmg = tg.getViewModeGlyph();
					int iteration = 0;
					if (vmg.isSelected()) {
						mgr.select(tg);
						mgr.doGraphSelections(0 == iteration);
						iteration += 1;
					}
				}
			}
		});
	}
}
