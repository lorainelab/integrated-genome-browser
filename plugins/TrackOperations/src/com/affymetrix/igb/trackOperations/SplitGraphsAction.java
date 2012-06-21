package com.affymetrix.igb.trackOperations;

import java.awt.event.ActionEvent;
import java.util.List;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.shared.ViewModeGlyph;

/**
 *  Puts all selected graphs in separate tiers by setting the
 *  combo state of each graph's state to null.        
 */
public class SplitGraphsAction extends GenericAction {
	private static final long serialVersionUID = 1l;
	private static final SplitGraphsAction ACTION = new SplitGraphsAction();
	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static SplitGraphsAction getAction() {
		return ACTION;
	}

	private SplitGraphsAction() {
		super(TrackOperationsTab.BUNDLE.getString("splitButton"), null, null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		List<ViewModeGlyph> selectedGlyphs = TrackOperationsTab.getSingleton().getSelectedGlyphss();
		for (ViewModeGlyph vg : selectedGlyphs) {
			TrackOperationsTab.getSingleton().getIgbService().deselect(vg.getTierGlyph());
			for (GlyphI gl : vg.getChildren()) {
				GraphSym gsym = (GraphSym)gl.getInfo();
				GraphState gstate = gsym.getGraphState();
				gstate.setComboStyle(null, 0);

				// For simplicity, set the floating state of all new tiers to false.
				// Otherwise, have to calculate valid, non-overlapping y-positions and heights.
				gstate.getTierStyle().setFloatTier(false); // for simplicity
			}
		}
		TrackOperationsTab.getSingleton().updateViewer();
	}
}
