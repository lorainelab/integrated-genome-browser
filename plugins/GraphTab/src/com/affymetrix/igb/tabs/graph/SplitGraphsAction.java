package com.affymetrix.igb.tabs.graph;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.util.ThreadUtils;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.shared.GraphGlyph;

import static com.affymetrix.igb.shared.Selections.*;
/**
 *  Puts all selected graphs in separate tiers by setting the
 *  combo state of each graph's state to null.        
 */
public class SplitGraphsAction extends GenericAction {
	private static final long serialVersionUID = 1l;

	public SplitGraphsAction(IGBService igbService) {
		super("Split", null, null);
		this.igbService = igbService;
	}

	private final IGBService igbService;

	@Override
	public void actionPerformed(java.awt.event.ActionEvent e) {
		super.actionPerformed(e);
		
		for (GraphGlyph gg : graphGlyphs) {
			if(gg.getParent().getChildCount() == 2){
				for(int i=0; i<gg.getParent().getChildCount(); i++){
					if(gg.getParent().getChild(i) instanceof GraphGlyph){
						split((GraphGlyph)gg.getParent().getChild(i));
					}
				}
			}else{
				split(gg);
			}
		}
		//igbService.getSeqMapView().postSelections();
		updateDisplay();
	}
	private void updateDisplay() {
		ThreadUtils.runOnEventQueue(new Runnable() {
	
			public void run() {
//				igbService.getSeqMap().updateWidget();
//				igbService.getSeqMapView().setTierStyles();
//				igbService.getSeqMapView().repackTheTiers(true, true);
				igbService.getSeqMapView().updatePanel(true, true);
			}
		});
	}

	private void split(GraphGlyph gg) {
		GraphSym gsym = (GraphSym) gg.getInfo();
		GraphState gstate = gsym.getGraphState();
		gstate.setComboStyle(null, 0);
		gstate.getTierStyle().setJoin(false);
//			igbService.selectTrack(child, true);

		// For simplicity, set the floating state of all new tiers to false.
		// Otherwise, have to calculate valid, non-overlapping y-positions and heights.
		gstate.getTierStyle().setFloatTier(false); // for simplicity
	}
}
