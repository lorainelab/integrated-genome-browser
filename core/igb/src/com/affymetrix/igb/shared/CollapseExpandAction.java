package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.action.CollapseAction;
import com.affymetrix.igb.action.ExpandAction;
import com.affymetrix.igb.action.SeqMapToggleAction;
import com.affymetrix.igb.action.SeqMapViewActionA;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.viewmode.AnnotationGlyph;
import java.util.List;

public class CollapseExpandAction extends SeqMapToggleAction implements SymSelectionListener {
	private static final long serialVersionUID = 1L;
	private static final CollapseExpandAction ACTION =
		new CollapseExpandAction(
			ExpandAction.getAction(),
			CollapseAction.getAction()
		);

	/**
	 * Load the class and, so, run the static code
	 * which creates a singleton.
	 * Add it to the {@link GenericActionHolder}.
	 * This is in lieu of the static {@link #getAction} method
	 * used by other actions.
	 */
	public static void createSingleton() {
		GenericActionHolder.getInstance().addGenericAction(ACTION);
		GenometryModel.getGenometryModel().addSymSelectionListener(ACTION);
		ACTION.setEnabled(false); // until such time as a track is selected.
	}

	protected CollapseExpandAction(SeqMapViewActionA a, SeqMapViewActionA b) {
		super(a, b);
	}

	@Override
	public void symSelectionChanged(SymSelectionEvent evt) {
		Object o = evt.getSource();
		SeqMapView gviewer = getSeqMapView();
		if (!(o == gviewer || o == gviewer.getSeqMap())) {
			return;
		}

		// Suppress unchecked warning when casting the list.
		// In this case the warning is over zealous.
		@SuppressWarnings("unchecked")
		List<GlyphI> l = (List<GlyphI>) gviewer.getSelectedTiers();
		List<SeqSymmetry> selected_syms = SeqMapView.glyphsToSyms(l);
		// Only pay attention to selections from the main SeqMapView or its map.
		// Ignore the splice view as well as events coming from this class itself.

		changeActionDisplay(selected_syms);
	}

	private void changeActionDisplay(List<SeqSymmetry> selected_syms) {
		boolean hasCollapsed = false;
		boolean hasExpanded = false;
		for (TierGlyph tg : getSeqMapView().getTierManager().getVisibleTierGlyphs()) {
			ViewModeGlyph vg = tg.getViewModeGlyph();
			if (vg instanceof AnnotationGlyph && !(vg instanceof MultiGraphGlyph)) {
				SeqSymmetry ss = (SeqSymmetry) vg.getInfo();
				if (selected_syms.contains(ss)) {
					boolean collapsed = vg.getAnnotStyle().getCollapsed();
					hasCollapsed |= collapsed;
					hasExpanded |= !collapsed;
				}
			}
		}
		// Note that the following depends on the client actions being singletons.
		ExpandAction.getAction().setEnabled(hasCollapsed);
		CollapseAction.getAction().setEnabled(hasExpanded);
	}

}
