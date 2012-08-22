package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.igb.shared.GraphGlyph;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.tiers.TierLabelManager;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.SwingUtilities;
import static com.affymetrix.igb.shared.Selections.*;

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
			List<GraphGlyph> selectedTiers = getSeqMapView().getFloatingGraphGlyphs();
			for (GraphGlyph vg : selectedTiers) {
				//SeqSymmetry ss = (SeqSymmetry) vg.getInfo();
				//if (selected_syms.contains(ss)) { // Need this? Action doesn't.
				boolean floating = vg.getGraphState().getTierStyle().getFloatTier();
				hasFloater |= floating;
				hasAnchored |= !floating;
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
		for (GraphState state : graphStates) {
			ITrackStyleExtended style = state.getTierStyle();
			boolean is_floating = style.getFloatTier();
			if (is_floating) {
				//GraphGlyphUtils.attachGraph(gl, gviewer);
				// figure out correct height
//				Rectangle2D.Double tempbox = gl.getCoordBox();  // pixels, since in PixelFloaterGlyph 1:1 mapping of pixel:coord
//				Rectangle pixbox = new Rectangle((int) tempbox.x, (int) tempbox.y, (int) tempbox.width, (int) tempbox.height);
//				Rectangle2D.Double coordbox = new Rectangle2D.Double();
//				getSeqMapView().getSeqMap().getView().transformToCoords(pixbox, coordbox);
				
				style.setY(style.getY());
				style.setHeight(style.getHeight());
				
				style.setFloatTier(false);
				state.setShowLabel(false);
				
				something_changed = true;
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
				// Compensating for what I think is a bug in setAnnotatedSeq:
				// Select the label glyph to generate a selection change event.
				TierLabelManager mgr = v.getTierManager();
				List<TierLabelGlyph> labels = mgr.getAllTierLabels();
				for (TierLabelGlyph g : labels) {
					TierGlyph tg = g.getReferenceTier();
					int iteration = 0;
					if (tg.isSelected()) {
						mgr.select(tg);
						mgr.doGraphSelections(0 == iteration);
						iteration += 1;
					}
				}
			}
		});
	}
}
