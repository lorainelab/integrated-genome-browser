package com.affymetrix.igb.trackOperations;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.GraphTierGlyph;
import com.affymetrix.igb.shared.TierGlyph;

/**
 *  Puts all selected graphs in separate tiers by setting the
 *  combo state of each graph's state to null.        
 */
public class SplitGraphsAction extends GenericAction {
	private static final long serialVersionUID = 1l;

	public SplitGraphsAction(IGBService igbService) {
		super(TrackOperationsTab.BUNDLE.getString("splitButton"), null, null);
		this.igbService = igbService;
	}

	private final IGBService igbService;

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		@SuppressWarnings({ "unchecked", "rawtypes" })
		List<TierGlyph> selectedGlyphs = (List)igbService.getSeqMapView().getAllSelectedTiers();
		for (TierGlyph vg : selectedGlyphs) {
			if (vg instanceof GraphTierGlyph) {
				if (vg.getChildren() != null) {
					for (GlyphI gl : new CopyOnWriteArrayList<GlyphI>(vg.getChildren())) {
						GraphSym gsym = (GraphSym)gl.getInfo();
						GraphState gstate = gsym.getGraphState();
						gstate.setComboStyle(null, 0);
//						igbService.selectTrack(child, true);
		
						// For simplicity, set the floating state of all new tiers to false.
						// Otherwise, have to calculate valid, non-overlapping y-positions and heights.
						gstate.getTierStyle().setFloatTier(false); // for simplicity
					}
				}
			}
		}
		TrackOperationsTab.getSingleton().updateViewer();
		igbService.getSeqMapView().postSelections();
	}
}
